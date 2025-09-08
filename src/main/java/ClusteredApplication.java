/*
Copyright 2025 Gregory Ledenev (gregory.ledenev37@gmail.com)

MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the “Software”), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import com.gl.vertx.easyrouting.Application;
import com.gl.vertx.easyrouting.annotations.ClusterNodeURI;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static com.gl.vertx.easyrouting.annotations.HttpMethods.*;

/**
 * Sample clustered application demonstrating EasyRouting clustering and inter-node communication.
 */
public class ClusteredApplication extends Application {
    /**
     * Aggregates greetings from cluster nodes.
     * @param node1 URI of node1 if available
     * @param node2 URI of node2 if available
     * @return combined greetings from all nodes
     */
    @GET("/*")
    public String helloWorld(@ClusterNodeURI("node1") URI node1,
                             @ClusterNodeURI("node2") URI node2) {
        List<String> result;
        HttpClient client = HttpClient.newHttpClient();
        try {
            result = new ArrayList<>();

            result.add("Hello World!");
            appendHelloFromNode(client, node1, result);
            appendHelloFromNode(client, node2, result);
        } finally {
            client.close();
        }

        return String.join(", ", result);
    }

    /**
     * Returns greeting from this node.
     * @return a greeting message with node name
     */
    @GET("/helloFromNode")
    public String helloFromNode() {
        return "Hello from '%s'".formatted(getNodeName());
    }

    /**
     * Fetches greeting from a node and appends to a result list.
     * @param httpClient HTTP client for requests
     * @param node target node URI
     * @param result list to append greeting to
     */
    private void appendHelloFromNode(HttpClient httpClient, URI node, List<String> result) {
        if (node == null)
            return;

        try {
            HttpRequest request = HttpRequest.newBuilder().uri(node.resolve("/helloFromNode")).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            result.add(response.body());
        } catch (Exception e) {
            System.err.printf("Node is not available or failed to process request: %s%n", getNodeName());
        }
    }

    /**
     * Main entry point. Starts clustered application.
     * @param args [port] [nodeName] - optional port and node name
     */
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String nodeName = args.length > 0 ? args[1] : "node" + port;

        new ClusteredApplication()
                .clustered(nodeName)
                .start(port);
    }
}
