import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SoundsParser {
    private File rootDirectory = new File("./JesusRadio");
    private String baseUrl = "https://radio.fonki.pro";
    private File errorFile = new File(rootDirectory, "errorFile.txt");
    private PrintWriter out;
    private Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public SoundsParser() {
    }

    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (!Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }

    public void startParse() {
        deleteDir(rootDirectory);
        boolean newRootDir = rootDirectory.mkdir();
        try {
            boolean newErrorFile = errorFile.createNewFile();
            FileWriter fw = new FileWriter(errorFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            out = new PrintWriter(bw);
            int i = 1;
            List<RadioDto> result = new ArrayList<>();
            while (true) {
                Document doc = Jsoup.connect(baseUrl + "/st/" + i).get();
                Elements allPage = doc.body().select("*");
                Element radioBlocksList = allPage.select("div#all_entries").get(0);
                Elements radioBlocks = radioBlocksList.select("div.radio_block");
                if (radioBlocks.isEmpty())
                    break;
                ExecutorService service = Executors.newCachedThreadPool();
                radioBlocks.forEach(element -> service.submit(() -> parseRadioBlock(element, result)));
                try {
                    service.shutdown();
                    service.awaitTermination(1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log(String.format("page %s has parsed", i));
                i++;
            }
            listToJson(rootDirectory, result);
            log(String.format("FINISH total size %s", result.size()));

        } catch (IOException e) {
            out.println(String.format("Общая ошибка = %s", e.getMessage()));
            e.printStackTrace();
        }
    }

    private void parseRadioBlock(Element element, List<RadioDto> result) {
        Elements imageBlock = element.select("div.radio_block__image");
        String radioImage = imageBlock.attr("style");
        radioImage = radioImage.replace("background-image: url(", "");
        radioImage = radioImage.replace("small_", "");
        radioImage = radioImage.replace(")", "");
        radioImage = baseUrl + radioImage;
        String radioUrl = imageBlock.select("a").get(0).attr("href");
        String radioName = imageBlock.select("a").get(0).attr("data-name");
        addSynchronized(result, new RadioDto(radioName, radioUrl, radioImage));
        log(radioImage);
    }

    private void listToJson(File parentDirectory, List<RadioDto> list) {
        Type listType = new TypeToken<List<RadioDto>>() {
        }.getType();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(parentDirectory, "radios")));
            writer.write(gson.toJson(list, listType));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void addSynchronized(List<RadioDto> list, RadioDto item) {
        list.add(item);
    }

    private void log(String msg) {
        System.out.println("SoundsParser: " + msg);
    }
}
