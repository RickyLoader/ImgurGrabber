import java.io.*;
import java.util.Scanner;
import java.util.HashMap;
import java.util.ArrayList;
import java.net.URL;

/**
 * ImgurGrabber.java - Pulls all individual image URLs from an imgur.com album, can pull ~1000 links in <5 seconds.
 *
 * @author Ricky Loader
 * @version 1.0
 */
public class ImgurGrabber {

    /**
     * Prompts user for input of an imgur album URL and returns the direct image URLs of each image.
     *
     * @param args Command line input.
     */
    public static void main(String[] args) {

        /* Take an imgur album URL as input. */
        Scanner input = new Scanner(System.in);
        System.out.println("Please enter an IMGUR album link to obtain image URLs from:\n");
        String desired = input.nextLine().replace(" ","");

        /* Verify the URL is valid. */
        while (!isLink(desired, true)) {
            System.out.println("Invalid IMGUR url please try again:\n");
            System.out.println(desired);
            desired = input.nextLine().replace(" ","");
        }
        input.close();
        System.out.println("\nFetching links...\n");

        long startTime = System.currentTimeMillis();

        /* Pull the HTML from the URL. */
        String html = visitLink(desired);

        /* Find and create each image's URL from the HTML. */
        int links = findLinks(html);

        long endTime = System.currentTimeMillis();
        System.out.println("\nFound "+ links + " images in " + (endTime-startTime) +"ms.");
    }

    /**
     * Verify that a given imgur URL matches the correct format for either an album or an individual image.
     *
     * @param url   A String URL to be verified.
     * @param album A Boolean specifying whether the URL is for an album or not.
     * @return A Boolean specifying whether the URL is correct or not.
     */
    private static boolean isLink(String url, boolean album) {
        boolean result = false;
        /* REGEX for an imgur single image URL. */
        String linkFormat = "https://imgur.com/[a-zA-Z0-9]+";

        /* REGEX for an imgur album URL. */
        String albumFormat = "https://imgur.com/a/\\S+";

        /* Use a map to find appropriate REGEX for given Boolean. */
        HashMap<Boolean, String> formats = new HashMap<>();
        formats.put(true, albumFormat);
        formats.put(false, linkFormat);

        if (url.matches(formats.get(album))) {
            result = true;
        }
        return result;
    }

    /**
     * Visits the given URL and returns a String containing the HTML content found.
     *
     * @param desired The String URL to be visited.
     * @return A String containing the HTML content found.
     */
    private static String visitLink(String desired) {
        String html = "";

        try {
            URL url = new URL(desired);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {

                /* Filter out all HTML except the block containing image URLs. */
                if (line.contains("\"hash\"")) {
                    html += line;
                }
            }
            bufferedReader.close();
            return html;
        } catch (IOException e) {
            System.out.println("Unable to open URL");
        }
        return html;
    }

    /**
     * Searches a String containing HTML for each individual image URL. imgur only stores the images
     * currently visible on screen in <img src= > tags. The images are stored as objects with attributes
     * such as title, description, hash, etc. The hash attribute's value can be appended to an imgur prefix
     * to obtain a direct URL to the image.
     *
     * @param html A String containing the HTML to be searched.
     */
    private static int findLinks(String html) {

        /* The album's images are stored in two blocks within the HTML, each beginning with this prefix. */
        String albumRegex = "\"count\":[0-9]+,\"images\":\\[";

        /*
         * Splitting on albumRegex produces 3 Strings, the HTML prior to the first occurrence, and two duplicate
         * blocks of HTML containing the image objects of the album (only the first is needed).
         */
        String album = html.split(albumRegex)[1];

        /* Image objects are comma separated. */
        String[] imageObjects = album.split("},");

        ArrayList<String> unique = new ArrayList<>();

        /* Each image object begins with this String followed by the hash value and the remaining attributes. */
        String imageRegex = "{\"hash\":\"";

        /* The prefix for which to append a hash value to gain the image's URL. */
        String imgurPrefix = "https://imgur.com/";

        /* For each image object. */
        for (String object : imageObjects) {

            /*
             * Create the URL by removing the imageRegex which leaves the hash value and the remaining attributes.
             * Split on quote marks the end of the hash value. This leaves the hash value
             * in the 0 position which can then be appended to the imgurPrefix to gain the image's URL.
             */
            String url = imgurPrefix + (object.replace(imageRegex, "").split("\"")[0]);

            /* Verify that the URL is not a duplicate and is a valid imgur link. */
            if (!unique.contains(url) && isLink(url, false)) {
                unique.add(url);
                System.out.println(url);
            }
        }
        return unique.size();
    }
}
