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

public class ClusteredApplication extends Application {
    @GET("/*")
    public String helloWorld(@ClusterNodeURI("node1") URI node1,
                             @ClusterNodeURI("node2") URI node2) {
        HttpClient client = HttpClient.newHttpClient();
        List<String> result = new ArrayList<>();

        result.add("Hello World!");
        getHelloFromNode(client, node1, result);
        getHelloFromNode(client, node2, result);

        return String.join(", ", result);
    }

    @GET("/helloFromNode")
    public String helloFromNode() {
        return "Hello from '%s'".formatted(getNodeName());
    }

    private void getHelloFromNode(HttpClient httpClient, URI node, List<String> result) {
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

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String nodeName = args.length > 0 ? args[1] : "node" + port;

        new ClusteredApplication()
                .clustered(nodeName)
                .start(port);
    }
}
