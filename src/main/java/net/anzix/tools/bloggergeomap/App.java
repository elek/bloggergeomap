package net.anzix.tools.bloggergeomap;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.BloggerRequest;
import com.google.api.services.blogger.model.Post;
import com.google.api.services.blogger.model.PostList;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;

/**
 * Simple CLI to fetch geodata links from a blogger blog.
 */
public class App {

    private Blogger b;

    @Option(name="-o",usage = "Output dir",metaVar = "DIR")
    File outputDir = new File(".");

    @Option(name = "-k",usage = "Google api key from http://code.google.com/apis/console",required = true)
    String key;

    @Option(name="-b",usage = "Identifier of the blog.",required = true)
    String blogId;


    public static void main(String[] args) throws Exception {

        App app = new App();
        CmdLineParser parser = new CmdLineParser(app);
        try {
            parser.parseArgument(args);
            app.run();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err
                    .println("java -jar bloggergeomap.jar [options...]");
            parser.printUsage(System.err);
            return;
        }


    }

    private void run() throws Exception {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        b = Blogger.builder(new NetHttpTransport(), new JacksonFactory()).setApplicationName("bloggergeomap").setJsonHttpRequestInitializer(new JsonHttpRequestInitializer() {
            @Override
            public void initialize(JsonHttpRequest request) throws IOException {
                BloggerRequest bloggerRequest = (BloggerRequest) request;
                bloggerRequest.setPrettyPrint(true);
                bloggerRequest.setKey(key);
            }
        }).build();

        writePoi();


    }

    private void writePoi() throws Exception {
        try(Writer writer = new OutputStreamWriter(new FileOutputStream(new File(outputDir, "pois.txt")),"UTF-8")){
            writer.write("lat\tlon\ttitle\tdescription\ticon\ticonSize\ticonOffset\n");
            fetch(writer, null);
            writer.write("\n");
            writer.close();
        }
    }

    public void fetch(Writer writer, String pageToken) throws Exception {
        Blogger.Posts.List posts = b.posts().list(blogId);
        if (pageToken != null) {
            posts.setPageToken(pageToken);
        }
        PostList result = posts.execute();

        for (Post p : result.getItems()) {
            if (p.getLocation() != null) {
                writer.write(p.getLocation().getLat() + "\t");
                writer.write(p.getLocation().getLng() + "\t");
                writer.write(p.getTitle() + "\t");
                writer.write(p.getContent().replaceAll("\\n", "") + "\t");
                writer.write("marker.png\t");
                writer.write("24,24\t");
                writer.write("0,0\n");
            }
        }
        if (result.getNextPageToken() != null) {
            fetch(writer, result.getNextPageToken());
        }
    }
}
