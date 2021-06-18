package com.github.pierre_ernst.github_api_net.model;

import java.util.Objects;

public class GHPackage implements Comparable<GHPackage> {

	private String id;
	private String name;

	public GHPackage(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "GHPackage [id=" + id + ", name=" + name + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GHPackage other = (GHPackage) obj;
		return Objects.equals(id, other.id) && Objects.equals(name, other.name);
	}

	@Override
	public int compareTo(GHPackage other) {
		return this.name.compareTo(other.name);
	}
}
