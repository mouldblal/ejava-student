package ejava.examples.ejbwar.inventory.client;

import java.net.URI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.j2ee.statistics.StatefulSessionBeanStats;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ejava.examples.ejbwar.inventory.bo.Categories;
import ejava.examples.ejbwar.inventory.bo.Category;
import ejava.examples.ejbwar.inventory.bo.Product;
import ejava.examples.ejbwar.inventory.bo.Products;
import ejava.examples.ejbwar.inventory.rs.CategoriesResource;
import ejava.examples.ejbwar.inventory.rs.ProductsResource;
import ejava.examples.ejbwar.jaxrs.JAXBUtils;

/**
 * This class implements a JAX-RS Client interface to the inventory 
 * web application. All commands are through HTTP POST, GET, PUT, and DELETE
 * methods to specific resource URIs for products and categories. However,
 * this uses an API interface that hides some of the HTTP and marshaling details.
 */
public class InventoryJaxRSClientImpl implements InventoryClient {
	private static final Logger logger = LoggerFactory.getLogger(InventoryJaxRSClientImpl.class);
	private Client client = ClientBuilder.newClient();
	/**
	 * Defines the HTTP URL for the WAR that hosts the JAX-RS resources.
	 */
	private URI appURI;

	public void setAppURI(URI appURI) {
		this.appURI = appURI;
	}
	
	private boolean isSuccessful(Response response) {
	    return response.getStatusInfo().getFamily() == Family.SUCCESSFUL;
	}
	
	/**
	 * Helper method that returns a URIBuilder fully initialized to point
	 * to the URI that will reach the specified method within the inventory
	 * resource classes.
	 * @param resourceClass
	 * @param method
	 * @return
	 */
	protected <T> UriBuilder buildURI(String path) {
		//start with the URI for the WAR deployed to the server 
		//that ends with the context-root
		return UriBuilder.fromUri(appURI)
				//add path info from the 
				//javax.ws.rs.core.Application @ApplicationPath
				.path("rest")
				//add in @Path added by resource class and method
				.path(path);
	}
		
	@Override
	public Categories findCategoryByName(String name, int offset, int limit)  {
		//build a URI to the specific method that is hosted within the app
		URI uri = buildURI("categories")
				//marshall @QueryParams into URI
				.queryParam("name", name)
				.queryParam("offset", offset)
				.queryParam("limit", limit)
				.build();
		
        //build the overall request 
		WebTarget target = client.target(uri);
		target = target.register(Categories.class);
		Builder request = target.request(MediaType.APPLICATION_XML_TYPE);
		Invocation get = request.buildGet();

        //issue request and look for an OK response with entity
		try (Response response = get.invoke(Response.class)) {
	        logger.debug("GET {} returned {}", uri, response.getStatusInfo());
	        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
	            return response.readEntity(Categories.class);
		    } else {
		        String payload = (response.hasEntity()) ? response.readEntity(String.class) : "";
	            throw new ResponseProcessingException(response, payload);		        
		    }
		}	
	}
	
	@Override
	public Category getCategory(int id) throws ResponseProcessingException {
		URI uri = buildURI("categories/{id}")
				//marshall @PathParm into the URI
				.build(id);
		
		//build the overall request
		Builder request = client.target(uri)
		                        .request(MediaType.APPLICATION_XML_TYPE);
		
		//issue request and look for an OK response with entity
		try (Response response = request.get()) {
            logger.debug("GET {} returned {}", uri, response.getStatusInfo());
        		if (isSuccessful(response)) {
        			return response.readEntity(Category.class);
        		} else {
                String payload = (response.hasEntity()) ? response.readEntity(String.class) : "";
                throw new ResponseProcessingException(response, payload);               
        		}
		}
	}
	
	@Override
	public boolean deleteCategory(int id) {
		URI uri = buildURI("category/{id}")
				//marshall @PathParm into the URI
				.build(id);
		
		//build and execute the overall request
		 Builder request = client.target(uri)
		                        .request();

		//issue request and look for an OK response without an entity
		try (Response response=request.delete()) {
		    logger.debug("DELETE {} returned {}", uri, response.getStatusInfo());
            if (isSuccessful(response)) {
    			    return true;
            } else {
                String payload = (response.hasEntity()) ? response.readEntity(String.class) : "";
                throw new ResponseProcessingException(response, payload);               
            }
		}
	}
	
	/**
	 * This method uses HTML FORM mechanism to POST a new product in the
	 * inventory. 
	 */
	@Override
	public Product createProduct(Product product, String categoryName) {
		URI uri = buildURI("products")
				//no @PathParams here
				.build();

		//build the form data with the request parameters
		Form form = new Form();
		form.param("name", product.getName());
		form.param("category", categoryName);
		if (product.getQuantity()!=null) {
			form.param("quantity", product.getQuantity().toString());
		}
		if (product.getPrice() != null) {
			form.param("price", product.getPrice().toString());
		}

		//create the request
		Invocation request = client.target(uri)
		        .request(MediaType.APPLICATION_XML)
		        .buildPost(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
			
		//issue the request and check the response
		try (Response response=request.invoke()) {
            logger.debug("POST {} returned {}", uri, response.getStatusInfo());
            if (isSuccessful(response)) {
    			    return response.readEntity(Product.class);
            } else {
                String payload = (response.hasEntity()) ? response.readEntity(String.class) : "";
                throw new ResponseProcessingException(response, payload);               
            }
		}
	}
	
	@Override
	public Products findProductsByName(String name, int offset, int limit) {
		URI uri = buildURI("products")
				//marshall @QueryParams into URI
				.queryParam("name", name)
				.queryParam("offset", offset)
				.queryParam("limit", limit)
				.build();
			
		//build the overall request
		Invocation request = client.target(uri)
		        .request(MediaType.APPLICATION_XML_TYPE)
		        .buildGet();
		
		//issue request and look for OK response with entity
		try (Response response=request.invoke()) {
            logger.debug("GET {} returned {}", uri, response.getStatusInfo());
            if (isSuccessful(response)) {
    			    return response.readEntity(Products.class);
            } else {
                String payload = (response.hasEntity()) ? response.readEntity(String.class) : "";
                throw new ResponseProcessingException(response, payload);               
            }
		}
	}
	
	@Override
	public Product getProduct(int id) {
		URI uri = buildURI("products/{id}")
				//marshal @PathParm into the URI
				.build(id);
			
		//build and execute overall request
		Invocation request = client.target(uri)
		        .request(MediaType.APPLICATION_XML_TYPE)
		        .buildGet();
		
		//issue request look for OK response with entity
		try (Response response=request.invoke()) {
            logger.debug("GET {} returned {}", uri, response.getStatusInfo());
            if (isSuccessful(response)) {
    			    return response.readEntity(Product.class);
            } else {
                String payload = (response.hasEntity()) ? response.readEntity(String.class) : "";
                throw new ResponseProcessingException(response, payload);               
            }
		}
	}
	
	@Override
	public Product updateProduct(Product product) {
		URI uri = buildURI("products/{id}")
				//marshal @PathParm into the URI
				.build(product.getId());
			
		//build overall request
		Invocation request = client.target(uri)
		        .request(MediaType.APPLICATION_XML_TYPE)
		        .buildPut(Entity.entity(product, MediaType.APPLICATION_XML_TYPE));
		
		//issue request and look for OK with entity
		try (Response response=request.invoke()) {
            logger.debug("PUT {} returned {}", uri, response.getStatusInfo());
            if (isSuccessful(response)) {
    			    return response.readEntity(Product.class);
            } else {
                String payload = (response.hasEntity()) ? response.readEntity(String.class) : "";
                throw new ResponseProcessingException(response, payload);               
            }
		}
	}

	@Override
	public boolean deleteProduct(int id) {
		URI uri = buildURI("products/{id}")
				//marshal @PathParm into the URI
				.build(id);
			
		//build and execute overall request
		Invocation request = client.target(uri)
		        .request()
		        .buildDelete();

		//issue request look for OK response without and entity
		try (Response response=request.invoke()) {
            logger.debug("DELETE {} returned {}", uri, response.getStatusInfo());
            if (isSuccessful(response)) {
    			    return true;
            } else {
                String payload = (response.hasEntity()) ? response.readEntity(String.class) : "";
                throw new ResponseProcessingException(response, payload);               
            }
		}
	}
}