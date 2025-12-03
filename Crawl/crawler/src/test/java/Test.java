

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * A simple example, used on the jsoup website.
 <p>To invoke from the command line, assuming you've downloaded the jsoup-examples
 jar to your current directory:</p>
 <p><code>java -cp jsoup-examples.jar org.jsoup.examples.Wikipedia url</code></p>
 */
public class Test {
    public static void main(String[] args) throws IOException {
        Document doc = Jsoup.connect("https://en.wikipedia.org/").get();
        log(doc.title());

        Elements newsHeadlines = doc.select("#mp-itn b a");
        for (Element headline : newsHeadlines) {
            log("%s\n\t%s", headline.attr("title"), headline.absUrl("href"));
        }

        System.out.println(String.format("https://timkiem.vnexpress.net/%s&page=%d", 
                                "Bao lu Dak Lak", 1));
        
    }

    private static void log(String msg, String... vals) {
        System.out.println(String.format(msg, (Object[]) vals));
    }
}