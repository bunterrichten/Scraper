import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Random;

//Alpha Vantage API Key: ONUAG8JT96OYTH92


public class Scraper {

    static FileContentReader fcr;

    public static void main(String[] args) {

        ArrayList<String> alreadyCheckedLinks = new ArrayList<String>();

        fcr = new FileContentReader();

        ArrayList<String> jobList = new ArrayList<String>(); // Liste zum Speichern aller Jobs
        boolean nextPage = true; // gibt es eine "Weitere"-Seite?
        int counter = 0; // Zähle Anzahl an gefundenen Jobs
        Elements rows;
        Element oneRow;
        String linkBase = "https://www.willhaben.at"; // Basis-Link OHNE Unterseiten
        String nextLinkPart = "/jobs/suche?location=Kirchdorf%20an%20der%20Krems&region=13978"; // Unterseite, die als erstes besucht wird
        String nextLink = linkBase + nextLinkPart; // baue gesamten Link aus Basis-Link und Unterseite zusammen
        Document doc;
        do {
            try {
                doc = Jsoup.connect(nextLink).get(); // verbinde mit Link
            } catch (Exception e) {
                System.out.println("PROBLEM!!!"); // Wenn irgendwas nicht funktioniert
                e.printStackTrace();
                doc = null;
            }

            if (doc != null) {
                // Verbindung hat funktioniert

                rows = doc.select(".Box-sc-wfmb7k-0 .sc-6686be3a-1"); // Wähle eine Anzahl an Elementen mit diesen Klassen aus

                for (int i = 0; i < rows.size(); i++) { // für jedes ausgewählte Element
                    oneRow = rows.get(i); // oneRow === ausgewähltes Element in diesem Schleifendurchlauf

                    Element job = oneRow.select(".sc-6686be3a-3").last(); // Suche Element mit Jobname in diesem Block
                    Element company = oneRow.select(".sc-ca51e2d8-0").last(); // Suche Element mit Unternehmensname in diesem Block
                    Element meta = oneRow.select(".Text-sc-10o2fdq-0").last(); // Suche Element mit Extra-Infos in diesem Block

                    String jobName = job.text(); // finde tatsächlichen Jobname als Text in dem Element

                    String companyName = company.text(); // finde Firmennahmen in dem Block
                    companyName = companyName.substring(0, companyName.length() - 5); // Entferne "Jobs" am Ende

                    String[] metaParts = meta.text().split(",|\\|"); // Finde zusätzliche Infos, zerteile Text bei , und |
                    String date = metaParts[0]; // 1. Teil: Datum
                    String amount = metaParts[1]; // 2. Teil: Teilzeit oder Vollzeit
                    String location = metaParts[2]; // 3. Teil: Wo ist dieser Job?

                    String companyRow = // companyRow === ganze Zeile mit Job-Daten, getrennt durch ein ;
                            replaceUmlaute( // Mache z.B. ä zu ae
                                    jobName + ";" +
                                            companyName + ";" +
                                            location + ";" +
                                            amount + ";" +
                                            date
                            );


                    System.out.println(companyRow); // Ausgabe

                    jobList.add(companyRow); // Füge Zeile zu Liste hinzu
                    System.out.println("---------------------");
                }

                // ---------------

                Element nextOne = doc.select(".Pagination__PaginationButton-sc-zvrf30-1").last(); // Wähle "Weiter"-Button am Ende der Seite aus
                nextLinkPart = nextOne.attr("href"); // Finde heraus, wohin der Button führt und speichere es


                if (nextLinkPart.length() == 0) { // Wenn es keinen "Weiter"-Button gibt
                    nextPage = false; // dann höre auf
                } else {
                    nextLink = linkBase + nextLinkPart; // sonst erstelle einen neuen Link zum besuchen (z.B. Seite 2)
                    System.out.println(counter + ": " + nextLink);
                }

                counter++; // Zähle +1 Jobs

                System.out.println("---------- GO TO NEXT PAGE ---------");
            }
        } while (nextPage);

        // Ausgabe am Ende, z.B. 23 Jobs
        System.out.println(jobList.size() + " Jobs");

        try {
            FileWriter fw = new FileWriter("jobList.csv");


            for (int i = 0; i < jobList.size(); i++) {
                fw.write(jobList.get(i) + "\n");
            }

            fw.close();

            System.out.println("DONE!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler: Datei nicht gefunden");
        }

    }

    private static String replaceUmlaute(String output) {
        String newString = output.replace("\u00fc", "ue")
                .replace("&amp;", "&")
                .replace("\u00f6", "oe")
                .replace("\u00e4", "ae")
                .replace("\u00df", "ss")
                .replaceAll("\u00dc(?=[a-z\u00e4\u00f6\u00fc\u00df ])", "Ue")
                .replaceAll("\u00d6(?=[a-z\u00e4\u00f6\u00fc\u00df ])", "Oe")
                .replaceAll("\u00c4(?=[a-z\u00e4\u00f6\u00fc\u00df ])", "Ae")
                .replace("\u00dc", "UE")
                .replace("\u00d6", "OE")
                .replace("\u00c4", "AE");
        return newString;
    }

    public static int[] getPSIscores(String link) {
        if (link.length() == 0)
            return new int[]{-1, -1};
        int[] scores = new int[2];
        Random r = new Random();
            /*Connection connection = Jsoup.connect("https://www.googleapis.com/pagespeedonline/v5/runPagespeed?key=AIzaSyCHDFi2I864kUaYOv0njrZJXqlo3a-TBLM&url=" + link + "%2F&fields=lighthouseResult%2Fcategories%2F*%2Fscore&strategy=desktop&category=performance");
            connection.timeout(20 * 1000);
            String test = connection.ignoreContentType(true).execute().body();
            System.out.println(test);*/
        try {
            String test = fcr.getContentFromUrl("https://www.googleapis.com/pagespeedonline/v5/runPagespeed?key=AIzaSyCHDFi2I864kUaYOv0njrZJXqlo3a-TBLM&url=" + link + "%2F&fields=lighthouseResult%2Fcategories%2F*%2Fscore&strategy=desktop&category=performance");

            scores[0] = (int) (Double.parseDouble(test.split("\"performance\": \\{\n        \"score\": ")[1].split("}")[0].trim()) * 100);

            // sleep for 0.5s to watch out for api limits
            try {
                Thread.sleep((long) (300));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            test = fcr.getContentFromUrl("https://www.googleapis.com/pagespeedonline/v5/runPagespeed?key=AIzaSyCHDFi2I864kUaYOv0njrZJXqlo3a-TBLM&url=" + link + "%2F&fields=lighthouseResult%2Fcategories%2F*%2Fscore&strategy=desktop&category=performance");

            //System.out.println(test);
            scores[1] = (int) (Double.parseDouble(test.split("\"performance\": \\{\n        \"score\": ")[1].split("}")[0].trim()) * 100);
            //System.out.println(test);
            // sleep for 0.5s to watch out for api limits
            try {
                Thread.sleep((long) (300));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Ergebnis: " + scores[0] + "/" + scores[1]);
        } catch (Exception e) {
            System.out.println("Exception cought!");
            e.printStackTrace();
            return new int[]{-2, -2};
        }
        //System.out.println(test);


        return scores;
    }
}
