package com.github.pierre_ernst.github_api_net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import com.github.pierre_ernst.github_api_net.model.GHPackage;

class LibraryTest {

	private GitHub getGitHubApiClient() throws IOException {
		return new GitHubBuilder().build();
	}

	private GHRepository GetRepo(GitHub gitHubApiClient, String owner, String name) throws IOException {

		return new GitHubHtmlClient(gitHubApiClient).getOwner(owner).getRepository(name);
	}

	@Test
	void listPackagesTest() {

		try {
			GitHub gitHubApiClient = getGitHubApiClient();
			GHRepository repo = GetRepo(gitHubApiClient, "FasterXML", "jackson-dataformats-binary");
			GitHubHtmlClient classUnderTest = new GitHubHtmlClient(gitHubApiClient);

			SortedSet<GHPackage> expected = new TreeSet<>();
			expected.add(new GHPackage("UGFja2FnZS0xODAwNDIzMjY=",
					"com.fasterxml.jackson.dataformat:jackson-dataformat-avro"));
			expected.add(new GHPackage("UGFja2FnZS0xODEwNzEyMjI=",
					"com.fasterxml.jackson.dataformat:jackson-dataformat-cbor"));
			expected.add(new GHPackage("UGFja2FnZS0yNTUyODg0ODc=",
					"com.fasterxml.jackson.dataformat:jackson-dataformat-ion"));
			expected.add(new GHPackage("UGFja2FnZS0xODE3OTQxNjc=",
					"com.fasterxml.jackson.dataformat:jackson-dataformat-protobuf"));
			expected.add(new GHPackage("UGFja2FnZS0xODAwNTc1NzM=",
					"com.fasterxml.jackson.dataformat:jackson-dataformat-smile"));
			expected.add(new GHPackage("UGFja2FnZS0yNTUyODg0ODQ=",
					"com.fasterxml.jackson.dataformat:jackson-dataformats-binary"));

			SortedSet<GHPackage> generated = new TreeSet<>(classUnderTest.listPackages(repo));

			assertEquals(expected, generated);

		} catch (IOException ex) {
			ex.printStackTrace(System.err);
			fail(ex);
		}
	}

	@Test
	void getDependentsCountTest() {
		try {
			GitHub gitHubApiClient = getGitHubApiClient();
			GHRepository repo = GetRepo(gitHubApiClient, "FasterXML", "jackson-dataformats-binary");
			GitHubHtmlClient classUnderTest = new GitHubHtmlClient(gitHubApiClient);

			assertTrue(classUnderTest.getDependentsCount(repo, "UGFja2FnZS0yNTUyODg0ODc=") > 120);

			assertTrue(
					classUnderTest.getDependentsCount(GetRepo(gitHubApiClient, "FasterXML", "jackson-core")) > 190000);

		} catch (IOException | ParseException ex) {
			ex.printStackTrace(System.err);
			fail(ex);
		}
	}

	@Test
	void listDependentsTest() {
		try {
			GitHub gitHubApiClient = getGitHubApiClient();
			GHRepository repo = GetRepo(gitHubApiClient, "FasterXML", "jackson-dataformats-binary");
			GitHubHtmlClient classUnderTest = new GitHubHtmlClient(gitHubApiClient);

			assertTrue(classUnderTest.listDependents(repo, "UGFja2FnZS0yNTUyODg0ODc=", 0, false).size() > 100);

		} catch (IOException ex) {
			ex.printStackTrace(System.err);
			fail(ex);
		}
	}
}
