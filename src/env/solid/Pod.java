package solid;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A CArtAgO artifact that agent can use to interact with LDP containers in a Solid pod.
 */
public class Pod extends Artifact {

    private String podURL; // the location of the Solid pod 

  /**
   * Method called by CArtAgO to initialize the artifact. 
   *
   * @param podURL The location of a Solid pod
   */
    public void init(String podURL) {
        this.podURL = podURL;
        log("Pod artifact initialized for: " + this.podURL);
    }

  /**
   * CArtAgO operation for creating a Linked Data Platform container in the Solid pod
   *
   * @param containerName The name of the container to be created
   * 
   */
    @OPERATION
    public void createContainer(String containerName) {
        log("creating new contrainer");

        //creating body
        String body = "@prefix ldp: <http://www.w3.org/ns/ldp#>.\n"+
                "@prefix dcterms: <http://purl.org/dc/terms/>.\n" +
                "<> a ldp:Container, ldp:BasicContainer, ldp:Resource;\n" +
                "dcterms:title \"" + containerName + "\";\n" +
                "dcterms:description \"Created container that will contain files for JaCoMo Application\".";
        createContainerRequest(body, containerName);
    }
        
    private boolean createContainerRequest(String body, String namePod) {
        String queryUrl = "https://solid.interactions.ics.unisg.ch/atilla/";

        try {
            URI uri = new URI(queryUrl);
            log("URI: " + uri.toString());
            log("query: " + body);
            log("name of pod" + namePod + "/");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"")
                    .header("Content-Type", "text/turtle")
                    .header("Slug", namePod + "/")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 201) {
                    log("status code " + (response.statusCode()));
                    throw new RuntimeException("HTTP error code : " + response.statusCode());
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return false;

    }
    
            

    

  /**
   * CArtAgO operation for publishing data within a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource will be created
   * @param fileName The name of the .txt file resource to be created in the container
   * @param data An array of Object data that will be stored in the .txt file
   */
    @OPERATION
    public void publishData(String containerName, String fileName, Object[] data) {
        log("adding data to the LDP container");


        URI fileUri = URI.create("https://solid.interactions.ics.unisg.ch/atilla/" + containerName + "/" + fileName);

        String body = "";
        for (Object obj : data) body += obj.toString() + "\n";

        try {
            HttpClient client = HttpClient.newHttpClient();
            //check if file already exists
            if(ressourceExists(fileUri)) {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(fileUri)
                    .header("Content-Type", "text/plain")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

                    client.send(request, HttpResponse.BodyHandlers.ofString());
            } else {
                //POST if not existing
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://solid.interactions.ics.unisg.ch/atilla/" + containerName + "/"))
                    .header("Content-Type", "text/plain")
                    .header("Slug", fileName)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

                    client.send(request, HttpResponse.BodyHandlers.ofString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }   
    }

    private Boolean ressourceExists(URI fileUri) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest getRequest = HttpRequest.newBuilder()
            .uri(fileUri)
            .GET()
            .build();

        try {
            HttpResponse<String> response = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() == 200){
                return true;
            } 
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
        
    }

  /**
   * CArtAgO operation for reading data of a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be read
   * @param data An array whose elements are the data read from the .txt file
   */
    @OPERATION
    public void readData(String containerName, String fileName, OpFeedbackParam<Object[]> data) {
        data.set(readData(containerName, fileName));
    }

  /**
   * Method for reading data of a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be read
   * @return An array whose elements are the data read from the .txt file
   */
    public Object[] readData(String containerName, String fileName) {

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest getRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://solid.interactions.ics.unisg.ch/atilla/" + containerName + "/" + fileName))
            .GET()
            .build();

        try {
            HttpResponse<String> response = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
            return createArrayFromString(response.body());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new Object[] { "" };
        

    }

  /**
   * Method that converts an array of Object instances to a string, 
   * e.g. the array ["one", 2, true] is converted to the string "one\n2\ntrue\n"
   *
   * @param array The array to be converted to a string
   * @return A string consisting of the string values of the array elements separated by "\n"
   */
    public static String createStringFromArray(Object[] array) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : array) {
            sb.append(obj.toString()).append("\n");
        }
        return sb.toString();
    }

  /**
   * Method that converts a string to an array of Object instances computed by splitting the given string with delimiter "\n"
   * e.g. the string "one\n2\ntrue\n" is converted to the array ["one", "2", "true"]
   *
   * @param str The string to be converted to an array
   * @return An array consisting of string values that occur by splitting the string around "\n"
   */
    public static Object[] createArrayFromString(String str) {
        return str.split("\n");
    }


  /**
   * CArtAgO operation for updating data of a .txt file in a Linked Data Platform container of the Solid pod
   * The method reads the data currently stored in the .txt file and publishes in the file the old data along with new data 
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be updated
   * @param data An array whose elements are the new data to be added in the .txt file
   */
    @OPERATION
    public void updateData(String containerName, String fileName, Object[] data) {
        Object[] oldData = readData(containerName, fileName);
        Object[] allData = new Object[oldData.length + data.length];
        System.arraycopy(oldData, 0, allData, 0, oldData.length);
        System.arraycopy(data, 0, allData, oldData.length, data.length);
        publishData(containerName, fileName, allData);
    }
}
