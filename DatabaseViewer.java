package PROJECT;
import java.util.*;
import IRUtilities.*;
import java.io.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;
import java.util.HashMap;
import java.lang.Math;
import jdbm.htree.HTree;
import java.io.Serializable;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

public class DatabaseViewer {
	private static Porter porter;
	private static HashSet<String> stopWords;
	private static Vector<String> searchTerms;
	private static HTree urlToId;
	private static HTree idToUrl;
	private static HTree wordToId;
	private static HTree idToWord;
	private static HTree titleForwardIndex;
	private static HTree docForwardIndex;
	private static HTree titleInvertedIndex;
	private static HTree docInvertedIndex;
	private static RecordManager recman;
    private static HTree bigram;
    private static HTree trigram;
    private static HTree unigram;
    private static HTree metadata;
    private static HTree parentLinkIndex;
	private static long recid;

    public DatabaseViewer() throws IOException {
    
		try {
			// Create stemmer and stop words
			porter = new Porter();

			stopWords = new HashSet<String>();
			FileReader fs = new FileReader("stopwords.txt");
			BufferedReader bs = new BufferedReader(fs);
			String word = bs.readLine();
			while(word != null){
				stopWords.add(word);
				word = bs.readLine();
			}

			// Load invertedIndexDoc
			recman = RecordManagerFactory.createRecordManager("Database");
			recid = recman.getNamedObject("invertedindex");

			if(recid == 0) {
				throw new IOException ("docInvertedIndex does not exist");
			} else {
				docInvertedIndex = HTree.load(recman, recid);
			}

			// Load forwardIndexDoc
			long recid = recman.getNamedObject("forwardindex");

			if(recid == 0) {
				throw new IOException ("docForwardIndex does not exist");
			} else {
				docForwardIndex = HTree.load(recman, recid);
			}

			// Load invertedIndexTitle
			recid = recman.getNamedObject("titleInvertedindex");

			if(recid == 0) {
				throw new IOException ("titleInvertedIndex does not exist");
			} else {
				titleInvertedIndex = HTree.load(recman, recid);
			}

			// Load forwardIndexTitle
			recid = recman.getNamedObject("titleForwardindex");

			if(recid == 0) {
				throw new IOException ("titleForwardIndex does not exist");
			} else {
				titleForwardIndex = HTree.load(recman, recid);
			}

			// Load docMappings
			recid = recman.getNamedObject("urlToId");

			if(recid == 0) {
				throw new IOException ("urlToId does not exist");
			} else {
				urlToId = HTree.load(recman, recid);
			}

			recid = recman.getNamedObject("idToUrl");

			if(recid == 0) {
				throw new IOException ("idToUrl does not exist");
			} else {
				idToUrl = HTree.load(recman, recid);
			}
//
			// Load wordMappings
			recid = recman.getNamedObject("wordToId");

			if(recid == 0) {
				throw new IOException ("wordToId does not exist");
			} else {
				wordToId = HTree.load(recman, recid);
			}

			recid = recman.getNamedObject("idToWord");

			if(recid == 0) {
				throw new IOException ("idToWord does not exist");
			} else {
				idToWord = HTree.load(recman, recid);
			}

			recid = recman.getNamedObject("bigram");

			if(recid == 0) {
				throw new IOException ("bigram does not exist");
			} else {
				bigram = HTree.load(recman, recid);
			}

			recid = recman.getNamedObject("trigram");

			if(recid == 0) {
				throw new IOException ("trigram does not exist");
			} else {
				trigram = HTree.load(recman, recid);
			}

			recid = recman.getNamedObject("unigram");

			if(recid == 0) {
				throw new IOException ("unigram does not exist");
			} else {
				unigram = HTree.load(recman, recid);
			}

			recid = recman.getNamedObject("metadata");

			if(recid == 0) {
				throw new IOException ("metadata does not exist");
			} else {
				metadata = HTree.load(recman, recid);
			}
			
			recid = recman.getNamedObject("parentLinks");

			if(recid == 0) {
				throw new IOException ("parentLinks does not exist");
			} else {
				parentLinkIndex = HTree.load(recman, recid);
			}

		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Error: A JDBM Database required for search funciton is missing");
		}
    }


    public static void view(HTree tree) throws IOException {
        System.out.println("Viewing entries"); 
        FastIterator iter = tree.keys(); 
        Object key; 
        while ((key = iter.next())!=null) {
            Object value = tree.get(key);
            System.out.println("Key: " + key + ", Value: " + value);

        }
    }

    public static void main(String[] args) throws IOException {
        DatabaseViewer dbv = new DatabaseViewer(); 
        dbv.view(parentLinkIndex);
    }


}

