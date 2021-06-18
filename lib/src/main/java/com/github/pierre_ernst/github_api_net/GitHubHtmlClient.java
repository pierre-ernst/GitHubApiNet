package com.github.pierre_ernst.github_api_net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import com.github.pierre_ernst.github_api_net.model.GHPackage;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GitHubHtmlClient {

	private static final Pattern COUNT_PATTERN = Pattern.compile("^\\s*([0-9,]+)\\s+Repositories\\s*$");
	private static final Pattern REPO_PATTERN = Pattern.compile("^\\s*([\\S]+)\\s*/\\s*([\\S]+)\\s*$");
	private static final Pattern PACKAGE_ID_PATTERN = Pattern.compile("[^\\?]+.*\\?package_id=([a-zA-Z0-9=]+).*");

	private static final Logger LOGGER = Logger.getLogger(GitHubHtmlClient.class.getName());

	private GitHub delegate;
	private OkHttpClient httpClient;

	/**
	 * 
	 * @param githubApiClient @see https://github-api.kohsuke.org/
	 */
	public GitHubHtmlClient(GitHub githubApiClient) {
		delegate = githubApiClient;
		httpClient = new OkHttpClient();
	}

	private Document loadPage(URL url) throws IOException {
		Request request = new Request.Builder().url(url).build();

		Response response = httpClient.newCall(request).execute();

		return Jsoup.parse(response.body().string());
	}

	private URL getHtmlUrl(GHRepository repo) throws MalformedURLException, UnsupportedEncodingException {
		return getHtmlUrl(repo, null);
	}

	private URL getHtmlUrl(GHRepository repo, String packageId)
			throws MalformedURLException, UnsupportedEncodingException {
		String url = repo.getHtmlUrl() + "/network/dependents";
		if (packageId != null) {
			url += "?package_id=" + URLEncoder.encode(packageId, "UTF-8");
		}
		return new URL(url);
	}

	private void listDependentsInternal(Set<GHRepository> set, URL url, GHRepository repo, int minDependents,
			boolean sameLanguage) throws IOException {

		LOGGER.log(Level.FINE, "Scanning {0} current size: {1} ...", new Object[] { url, set.size() });

		Document html = loadPage(url);

		html.select("div.Box-row > span").forEach(e -> {
			Matcher m = REPO_PATTERN.matcher(e.text());
			if (m.find() && m.groupCount() == 2) {
				String owner = m.group(1);
				String name = m.group(2);
				GHRepository dependent = null;

				try {
					dependent = getOwner(owner).getRepository(name);
				} catch (IOException ignored) {
					LOGGER.log(Level.WARNING, "Repository {0}/{1} not found.", new Object[] { owner, name });
				}

				if (dependent != null) {
					try {
						if (getDependentsCount(dependent) >= minDependents) {
							if (!sameLanguage || Objects.equals(repo.getLanguage(), dependent.getLanguage())) {
								set.add(dependent);
							} else {
								LOGGER.log(Level.WARNING, "Repository {0}/{1} language is not '{2}'.",
										new Object[] { owner, name, repo.getLanguage() });
							}
						} else {
							LOGGER.log(Level.FINE, "Missed threshold ({0}) for repository {1}/{2}.",
									new Object[] { minDependents, owner, name });
						}
					} catch (IOException | ParseException ex) {
						LOGGER.log(Level.WARNING, "Unable to count dependents for repository {0}/{1}.",
								new Object[] { owner, name });
					}
				}
			}
		});

		Element nextPageButton = html.select("a.btn:nth-child(2)").first();
		if (nextPageButton != null) {
			listDependentsInternal(set, new URL(nextPageButton.attr("href")), repo, minDependents, sameLanguage);
		}
	}

	/**
	 * Given a name, returns an org or user
	 * 
	 * @param loginOrOrg login name or org name
	 * @return
	 * @throws IOException
	 */
	public GHPerson getOwner(String loginOrOrg) throws IOException {
		GHPerson owner = null;
		try {
			// First, try to get an org
			owner = delegate.getOrganization(loginOrOrg);
		} catch (IOException ignored) {
			// Then, try to get a user
			owner = delegate.getUser(loginOrOrg);
		}
		return owner;
	}

	public Set<GHPackage> listPackages(GHRepository repo) throws IOException {
		Set<GHPackage> list = new HashSet<>();

		Document html = loadPage(getHtmlUrl(repo));
		for (Element e : html.select("a.select-menu-item")) {
			Matcher m = PACKAGE_ID_PATTERN.matcher(URLDecoder.decode(e.attr("href"), "UTF-8"));
			if (m.find()) {
				String packageId = m.group(1);
				Element description = e.selectFirst("span");
				if (description != null) {
					String name = description.text().trim();
					if (name != null && !name.isEmpty()) {
						list.add(new GHPackage(packageId, name));
					}
				}
			}
		}

		return list;
	}

	/**
	 * 
	 * @param repo GitHub repository to be scanned
	 * @return the number of repositories depending on the given repo
	 * @throws IOException
	 * @throws ParseException
	 */
	public long getDependentsCount(GHRepository repo) throws IOException, ParseException {
		return getDependentsCount(repo, null);
	}

	/**
	 * 
	 * @param repo      GitHub repository to be scanned
	 * @param packageId @see #listPackages(GHRepository)
	 * @return the number of repositories depending on the given repo and packageId
	 * @throws IOException
	 * @throws ParseException
	 */
	public long getDependentsCount(GHRepository repo, String packageId) throws IOException, ParseException {

		Document html = loadPage(getHtmlUrl(repo, packageId));
		Element e = html.select("a.btn-link:nth-child(1)").first();

		Matcher m = COUNT_PATTERN.matcher(e.text());

		if (m.find()) {
			return (long) NumberFormat.getInstance(Locale.US).parse(m.group(1));
		}

		return 0;
	}

	/**
	 * Returns a list of dependents Repos
	 * 
	 * @param repo          GitHub repository to be scanned
	 * @param minDependents Only dependents having at least this given minimum
	 *                      number of dependents themselves are retained.
	 * @param sameLanguage  Only dependents having the same main language than
	 *                      <code>repo</code> are retained.
	 * @return
	 * @throws IOException
	 */
	public Set<GHRepository> listDependents(GHRepository repo, int minDependents, boolean sameLanguage)
			throws IOException {
		return listDependents(repo, null, minDependents, sameLanguage);
	}

	/**
	 * Returns a list of dependents Repos
	 * 
	 * @param repo          GitHub repository to be scanned
	 * @param packageId     @see #listPackages(GHRepository)
	 * @param minDependents Only dependents having at least this given minimum
	 *                      number of dependents themselves are retained.
	 * @param sameLanguage  Only dependents having the same main language than
	 *                      <code>repo</code> are retained.
	 * @return
	 * @throws IOException
	 */
	public Set<GHRepository> listDependents(GHRepository repo, String packageId, int minDependents,
			boolean sameLanguage) throws IOException {

		Set<GHRepository> list = new HashSet<>();

		listDependentsInternal(list, getHtmlUrl(repo, packageId), repo, minDependents, sameLanguage);

		return list;
	}

	/**
	 * Returns a list of dependents Repos having the same main language than
	 * <code>repo</code>
	 * 
	 * @param repo          GitHub repository to be scanned
	 * @param minDependents Only dependents having at list this given minimum number
	 *                      of dependents themselves are retained.
	 * @return
	 * @throws IOException
	 */
	public Set<GHRepository> listDependents(GHRepository repo, int minDependents) throws IOException {
		return listDependents(repo, minDependents, true);
	}

	/**
	 * Returns a list of dependents Repos having the same main language than
	 * <code>repo</code>
	 * 
	 * @param repo          GitHub repository to be scanned
	 * @param packageId     @see #listPackages(GHRepository)
	 * @param minDependents Only dependents having at list this given minimum number
	 *                      of dependents themselves are retained.
	 * @return
	 * @throws IOException
	 */
	public Set<GHRepository> listDependents(GHRepository repo, String packageId, int minDependents) throws IOException {
		return listDependents(repo, packageId, minDependents, true);
	}

	/**
	 * Returns a list of dependents Repos having the same main language than
	 * <code>repo</code> and at least 1 dependents themselves
	 * 
	 * @param repo      GitHub repository to be scanned
	 * @param packageId @see #listPackages(GHRepository)
	 * @return
	 * @throws IOException
	 */
	public Set<GHRepository> listDependents(GHRepository repo, String packageId) throws IOException {
		return listDependents(repo, packageId, 1, true);
	}

	/**
	 * Returns a list of dependents Repos having the same main language than
	 * <code>repo</code> and at least 1 dependents themselves
	 * 
	 * @param repo GitHub repository to be scanned
	 * @return
	 * @throws IOException
	 */
	public Set<GHRepository> listDependents(GHRepository repo) throws IOException {
		return listDependents(repo, 1, true);
	}
}
