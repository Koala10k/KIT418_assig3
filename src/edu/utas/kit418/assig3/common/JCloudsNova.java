package edu.utas.kit418.assig3.common;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;

public class JCloudsNova implements Closeable {
	private final NovaApi novaApi;
	private final Set<String> regions;

	public static void createNewNode() throws IOException {
		Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule());

		String provider = "openstack-nova";
		String identity = "pt-12682:pengd@utas.edu.au"; // tenantName:userName
		String credential = "MzU1OTE3YjU3OWRkNzk5";
		JCloudsNova jCloudsNova = new JCloudsNova(provider, identity, credential, modules);

		try {
			jCloudsNova.listServers();
			jCloudsNova.createServer();
			jCloudsNova.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			jCloudsNova.close();
		}
	}

	public JCloudsNova(String provider, String identity, String credential, Iterable<Module> modules) {
		novaApi = ContextBuilder.newBuilder(provider).endpoint("https://keystone.rc.nectar.org.au:5000/v2.0/").credentials(identity, credential).modules(modules).buildApi(NovaApi.class);
		regions = novaApi.getConfiguredRegions();
	}

	private void createServer() {
		ImageApi imageApi = novaApi.getImageApi("Melbourne");
		String imgname = new String("mpimaster");
		for (Image img : imageApi.listInDetail().concat()) {
			if (imgname.equals(img.getName())) {

				System.out.println(img.getId() + " " + img.getName());
			}

		}
	}

	private void listServers() {
		for (String region : regions) {
			System.out.println("Servers in " + region);
		}
	}

	public void close() throws IOException {
		Closeables.close(novaApi, true);
	}
}
