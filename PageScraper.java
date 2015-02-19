import com.jaunt.*;
import com.jaunt.component.*;
import java.io.*;
import java.lang.String;
import java.util.HashSet;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class PageScraper{
  private static ArrayList<String> _parsedCSV = new ArrayList<String>();
  private static ArrayList<String> _results = new ArrayList<String>();

/* writes each line of the csv into a string[]. Then 
   stores it into ArrayList */
  static ArrayList<String> readCSV(String[] name) {
    String realName = name[0]; //purpose: to run from main in command
    BufferedReader input = null;
    try {
      input = new BufferedReader(new FileReader(realName));
      String readString = input.readLine();

      /* check for fun */
      if (readString == null) {
        throw new IllegalStateException("File is empty");
      }

      while(readString != null) {
        _parsedCSV.add(readString);
        readString = input.readLine();
      }
    } catch (FileNotFoundException e) {
      System.out.printf("could not find %s \n", realName);
    } catch (IOException e) {
      System.out.printf("problem reading from %s \n", realName);
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          /* ignore */
        }
      }
    }
    return _parsedCSV;
  }
 
 /* writes arraylist to a file. */ 
  static void writeCSV() {
    if (_parsedCSV.isEmpty()) {
      System.out.println("uploaded csv is empty");
      return;
    } else if (_results.isEmpty()) {
      System.out.println("results array is empty");
      return;
    }
    PrintStream output = null;
    try {
      String sep = ",";
      output = new PrintStream("results.csv");
      for (int i = 0; i < _parsedCSV.size(); i++) {
        output.print(_parsedCSV.get(i));
        output.print(sep);
        output.print(_results.get(i));
        output.println(); 
      }
    } catch (IOException e) {
      System.err.println("trouble writing to file" + e.getMessage());
    } finally {
      if (output != null) {
        output.close();
      }
    }
  }

/* Visits Url/about, finds mailto */
  static void search(String keyword) throws JauntException{
    Boolean found = false;
    UserAgent userAgent = new UserAgent();
    userAgent.settings.autoSaveAsHTML = false;
    userAgent.visit("https://google.com");
    userAgent.doc.apply(keyword);
    userAgent.doc.submit("Google Search");
    
    Elements search = userAgent.doc.findFirst("<h3 class=r>").findEvery("<a>");
    int counter = 0; //dumb hack to get the first link  
    for(Element link : search) {   
       if (counter == 1) break;
       userAgent.visit(link.getAt("href"));
       counter++;
    }
    Elements reallink;
    try {
     reallink = userAgent.doc.findFirst("<div class=_jFe>").findEvery("<a>");
    }catch(JauntException e) {
      _results.add("email not found");
      return; 
    }
    
     int counter1 = 0; 
     String temp = null;
     String result = null;
     for (Element a : reallink) {
       if (counter1 == 1) break;
       temp = a.getAt("href");
       counter1++;
     }
     String last = temp.substring(temp.length()-1, temp.length());
     if (last.equals("/")) {
       result = temp + "about/";
     } else {
       result = temp + "/about/";
     }
     System.out.println("currently searching url: " + result); //sanity check
     try {
       userAgent.visit(result);
     } catch(JauntException e) {
        _results.add("email not found");
        return; 
     }
     Elements emailSearch = userAgent.doc.findEvery("<a>");
     HashSet<String> linksOnTarget = new HashSet<String>();
     for(Element email : emailSearch) {
        String maybe = null;        
        try {
          maybe = email.getAt("href");
        }catch(JauntException e) {
          _results.add("email not found");
          return; 
        }
       if (maybe.toLowerCase().contains("@")) {
        linksOnTarget.add(maybe);
        } 
     }
     HashSet<String> linksOnTargetCleaned = new HashSet<String>();
     for (String s : linksOnTarget) {
       if (s.contains("mailto")) {
         found = true;
         linksOnTargetCleaned.add(s.replace("mailto:",""));
       }
     }

     if (found) {
       for(String s : linksOnTargetCleaned) {
        _results.add(s); 
       }
     } else {
      _results.add("email not found"); 
     }
  }

    public static void main(String[] args) throws JauntException{    
      readCSV(args);
      for (String s : _parsedCSV ) { 
        search(s);
      }
      writeCSV();
      System.out.println("\nThis program ran successfully. Check your current directory for results.csv");
    }
  }

