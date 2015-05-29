package edu.utas.kit418.assig3.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;

public class JCloudsNova implements Closeable {
	private final NovaApi novaApi;
	private final Set<String> regions;
	private final static int MAX = 2;
	private static int curNum = 1;

	public static void createNewNode(String[] args) throws Exception {
		if (curNum == MAX) {
			System.out.println("NodeCreater: MAX amount of Nodes has been created");
			throw new Exception();
		}
		JCloudsNova jcloudsNova = new JCloudsNova();
		try {
			jcloudsNova.listServers();
			jcloudsNova.createServer();
			jcloudsNova.close();
		} catch (Exception e) {
			throw e;
		} finally {
			jcloudsNova.close();
		}
	}

	public JCloudsNova() {
		Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule());

		String provider = "openstack-nova";
		String identity = "pt-12682:pengd@utas.edu.au"; // tenantName:userName
		String credential = "MzU1OTE3YjU3OWRkNzk5";

		novaApi = ContextBuilder.newBuilder(provider).endpoint("https://keystone.rc.nectar.org.au:5000/v2.0/").credentials(identity, credential).modules(modules).buildApi(NovaApi.class);
		regions = novaApi.getConfiguredRegions();
	}

	private void createServer() {
		curNum++;
		String nameser = "n" + curNum;
		ImageApi imageApi = novaApi.getImageApi("Melbourne");
		FlavorApi flavors = novaApi.getFlavorApi("Melbourne");
		String imageid = "";
		String imgname = new String("pengd_node");
		System.out.println("\n\n\n imagename");
		for (Image img : imageApi.listInDetail().concat()) {
			if (imgname.equals(img.getName())) {

				System.out.println(img.getId() + " " + img.getName());
				imageid = img.getId();
			}

		}
		System.out.println("\n\n\n flavorename");

		/*
		 * for(Flavor img1:flavors.listInDetail().concat()) {
		 * 
		 * 
		 * System.out.println(img1.getId()+ " "+img1.getName());
		 * 
		 * 
		 * 
		 * }
		 */
		String flavorid = "0";
		SecurityGroupApi secureApi = novaApi.getSecurityGroupApi("Melbourne").get();
		// System.out.println("\n\n"+secureApi.list());
		CreateServerOptions options1 = CreateServerOptions.Builder.keyPairName("NectarKey").availabilityZone("tasmania").securityGroupNames("SSH");
		ServerApi serverApi = novaApi.getServerApi("Melbourne");
		ServerCreated screa = serverApi.create(nameser, imageid, flavorid, options1);
	}

	private void listServers() {
		for (String region : regions) {
			// ServerApi serverApi = novaApi.getServerApi(region);

			System.out.println("Servers in " + region);

			/*
			 * for (Server server : serverApi.listInDetail().concat()) {
			 * System.out.println("  " +
			 * server.getStatus()+" "+server.getName()); }
			 */
		}
	}

	public void close() throws IOException {
		Closeables.close(novaApi, true);
	}
}
