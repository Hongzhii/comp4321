package PROJECT;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.lang.Math;
import jdbm.htree.HTree;
import java.io.Serializable;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;


public class SearchEngine
{
	private Porter porter;
	private static HashSet<String> stopWords;
	//private Vector<String> searchTerms;
	private HTree urlToId;
	private HTree idToUrl;
	private HTree wordToId;
	private HTree idToWord;
	private HTree titleForwardIndex;
	private HTree docForwardIndex;
	private HTree titleInvertedIndex;
	private HTree docInvertedIndex;
	private HTree bigram;
	private HTree trigram;
	private RecordManager recman;
	long recid;
    
    public SearchEngine(
            Porter porter,
            HashSet<String> stopWords,
            HTree urlToId,
            HTree wordToId,
            HTree idToWord,
            HTree titleForwardIndex,
            HTree docForwardIndex,
            HTree titleInvertedIndex,
            HTree docInvertedIndex,
            HTree bigram,
            HTree trigram,
            RecordManager recman,
            long recid)throws IOException 
    {
        this.porter = porter;
        this.stopWords = stopWords;
        this.urlToId = urlToId;
        this.wordToId = wordToId;
        this.idToWord = idToWord;
        this.titleForwardIndex = titleForwardIndex;
        this.docForwardIndex = docForwardIndex;
        this.titleInvertedIndex = titleInvertedIndex;
        this.docInvertedIndex = docInvertedIndex;
        this.bigram = bigram;
        this.trigram = trigram;
        this.recman = recman;    
        this.recid =recid;
    }

	public SearchEngine() throws IOException
	{
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

			// Load ngrams
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

		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Error: A JDBM Database required for search funciton is missing");
		}
	}
	private String getPhrase(String query) throws IOException {
		query = " " + query + " ";

		String[] tokens = query.split("\"");
		String phrase = "";

		if(tokens.length == 3) {
			String[] phraseTokens = tokens[1].split(" ");

			for(String phraseToken : phraseTokens) {
				phrase += porter.stripAffixes(phraseToken);
				phrase += " ";

				if(stopWords.contains(phraseToken)) {
					phrase = "";
					break;
				}
			}
		}

		phrase = phrase.stripTrailing();

		return phrase;
	}

	private Vector<Integer> getWords(String query) throws IOException {
		query = " " + query + " ";
		String[] tokens = query.split("\"");

		Vector<Integer> wordIds = new Vector<Integer> ();

		if(tokens.length == 3) {
			query = tokens[0] + tokens[2];
		}

		query = query.stripLeading().stripTrailing();

		String[] wordTokens = query.split(" ");

		Set<Integer> tempSet = new HashSet<>();
		
		for(String wordToken : wordTokens) {
			if(!stopWords.contains(wordToken)) {
				String stem = porter.stripAffixes(wordToken);
				tempSet.add((Integer) wordToId.get(stem));
			}
		}

		// Remove repeat ID values
		for(Integer id : tempSet) {
			wordIds.add(id);
		}

		return wordIds;
	}

	private int getMaxTfDoc(int wordId, int docId) throws IOException {
		Vector<Posting> wordPostingVector = (Vector<Posting>) docForwardIndex.get(docId);

		int result = 1;

		for(Posting wordPosting : wordPostingVector) {
			if(wordPosting.freq > result) {
				result = wordPosting.freq;
			}
		}

		return result;
	}

	private int getMaxTfTitle(int wordId, int docId) throws IOException {
		Vector<Posting> wordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);

		int result = 1;

		for(Posting wordPosting : wordPostingVector) {
			if(wordPosting.freq > result) {
				result = wordPosting.freq;
			}
		}

		return result;
	}

	private double getTermWeightDoc(int wordId, int docId, int TF, int N) throws IOException {
		Vector<Posting> docPostingList = (Vector<Posting>) docInvertedIndex.get(wordId);
		Vector<Posting> wordPostingList = (Vector<Posting>) docForwardIndex.get(wordId);

		// Score is derived from (Term Frequency * IDF) / (Doc Max Term Frequency)
		int N_hasTerm = docPostingList.size();
		double IDF = Math.log(N/N_hasTerm) / Math.log(2);
		int TF_max = getMaxTfDoc(wordId, docId);

		double score = (TF * IDF) / (TF_max);

		return score;
	}

	private double getTermWeightTitle(int wordId, int docId, int TF, int N) throws IOException {
		Vector<Posting> docPostingList = (Vector<Posting>) titleInvertedIndex.get(wordId);
		Vector<Posting> wordPostingList = (Vector<Posting>) titleForwardIndex.get(wordId);

		// Score is derived from (Term Frequency * IDF) / (Doc Max Term Frequency)
		int N_hasTerm = docPostingList.size();
		double IDF = Math.log(N/N_hasTerm) / Math.log(2);
		int TF_max = getMaxTfTitle(wordId, docId);

		double score = (TF * IDF) / (TF_max);

		return score;
	}

	private double getDocMagnitude(int docId, int N) throws IOException {
		Vector<Posting> wordPostingVector = (Vector<Posting>) docForwardIndex.get(docId);

		double magnitude = 0;

		for(Posting wordPosting : wordPostingVector) {
			magnitude += Math.pow(getTermWeightDoc(wordPosting.id, docId, wordPosting.freq, N), 2);
		}

		magnitude = Math.sqrt(magnitude);

		return magnitude;
	}

	private double getTitleMagnitude(int docId, int N) throws IOException {
		Vector<Posting> wordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);

		double magnitude = 0;

		for(Posting wordPosting : wordPostingVector) {
			magnitude += Math.pow(getTermWeightTitle(wordPosting.id, docId, wordPosting.freq, N), 2);
		}

		magnitude = Math.sqrt(magnitude);

		return magnitude;
	}

	private Vector<Vector<Object>> search(Vector<Integer> query, String phrase) throws IOException {
		// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG
		System.out.println("Query is: ");

		for(Integer id : query) {
			if(id == null) {
				continue;
			}
			System.out.print(idToWord.get(id) + "\t");
		}

		System.out.println("\nPhrase is:");
		System.out.println(phrase);
		// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG

		if(phrase == "") {
			HashMap<Integer, Double> consolidatedScores = new HashMap<>();

			// Title Constant for prioritizing title matches
			int TITLE_CONSTANT = 4;

			// Find the total number of terms
			int N_doc = 0;
			int N_title = 0;

			FastIterator iterTerms = docInvertedIndex.keys();
			Integer tempId;
			while ((tempId = (Integer) iterTerms.next()) != null) {
				N_doc++;
			}

			iterTerms = titleInvertedIndex.keys();
			while ((tempId = (Integer) iterTerms.next()) != null) {
				N_title++;
			}

			try {
				for(Integer wordId : query) {
					if(wordId == null) {
						continue;
					}

					Vector<Posting> docPostingVector = (Vector<Posting>) docInvertedIndex.get(wordId);
					Vector<Posting> titlePostingVector = (Vector<Posting>) titleInvertedIndex.get(wordId);

					if(docPostingVector != null) {
						for (Posting docPosting : docPostingVector) {
							int docId = docPosting.id;
							int freq = docPosting.freq;
							double termWeight = getTermWeightDoc(wordId, docId, freq, N_doc);
							double docMagnitude = getDocMagnitude(docId, N_doc);

							termWeight = termWeight / docMagnitude;

							// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG

//							if(termWeight > 0) {
//								System.out.println("MATCH FOUND IN DOC");
//								System.out.println("URL: " + idToUrl.get(docId));
//								System.out.println("TERM: " + idToWord.get(wordId));
//							}

							// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG

							if (consolidatedScores.containsKey(docId)) {
								double currentScore = consolidatedScores.get(docId);
								consolidatedScores.put(docId, currentScore + termWeight);
							} else {
								consolidatedScores.put(docId, termWeight);
							}
						}
					}

					if(titlePostingVector != null) {
						for (Posting titlePosting : titlePostingVector) {
							int docId = titlePosting.id;
							int freq = titlePosting.freq;
							double termWeight = getTermWeightTitle(wordId, docId, freq, N_doc);
							double titleMagnitude = getTitleMagnitude(docId, N_doc);

							termWeight = TITLE_CONSTANT * termWeight / titleMagnitude;

							// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG

//							if(termWeight > 0) {
//								System.out.println("MATCH FOUND IN TITLE");
//								System.out.println("URL: " + idToUrl.get(docId));
//								System.out.println("TERM: " + idToWord.get(wordId));
//							}

							// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG

							if (consolidatedScores.containsKey(docId)) {
								double currentScore = consolidatedScores.get(docId);
								consolidatedScores.put(docId, currentScore + termWeight);
							} else {
								consolidatedScores.put(docId, termWeight);
							}
						}
					}
				}
			} catch (IOException ex) {
				System.err.println(ex);
			}

			Set<Integer> keys = consolidatedScores.keySet();

			Vector<Vector<Object>> results = new Vector<Vector<Object>>();

			double queryMagnitude = Math.sqrt(query.size()); // Required for calculating cosine similarity

			for(Integer key : keys) {
				Vector pair = new Vector<>();
				pair.add(key);
				pair.add(consolidatedScores.get(key) / queryMagnitude);
				results.add(pair);
			}

			System.out.println(results.size());

			Collections.sort(results, new Comparator<Vector<Object>>() {
				@Override
				public int compare(Vector<Object> p1, Vector<Object> p2) {
					double diff = (double) p1.get(1) - (double) p2.get(1);
					if (diff < 0) {
						return 1;
					} else if (diff > 0) {
						return -1;
					} else {
						return 0;
					}
				}
			});

			return results;
		} else {
			String[] tokens = phrase.split(" ");

			HashSet<Integer> docList = new HashSet<>();

			RecordManager recman = RecordManagerFactory.createRecordManager("Database");
			HTree ngram;
			long recid;

			if(tokens.length == 2) {
				docList = (HashSet<Integer>) bigram.get(phrase);
			} else if(tokens.length == 3) {
				docList = (HashSet<Integer>) trigram.get(phrase);
			}

			if(docList == null) {
				Vector<Vector<Object>> results = new Vector<Vector<Object>>();
				return results;
			}

//			Vector<Pair> results = search(query, "");
//
//			for(Pair pair : results) {
//				if(!docList.contains(pair)) {
//					results.remove(pair);
//				}
//			}

			Vector<Vector<Object>> results = search(query, "");

			Iterator<Vector<Object>> iterator = results.iterator();
			while (iterator.hasNext()) {
				Vector<Object> pair = iterator.next();
				if (!docList.contains((Integer) pair.get(0))) {
					iterator.remove();
				}
			}

			for(Integer docId : docList) {
				int EXISTS = 0;

				for(Vector<Object> pair : results) {
					if(((int) pair.get(0)) == docId) {
						EXISTS = 1;
						break;
					}
				}

				if(EXISTS == 1) {
					continue; // If entry is already in results, skip adding it
				}

				Vector<Object> tempPair = new Vector<>();
				tempPair.add(docId);
				tempPair.add(0);

				results.add(tempPair);
			}

			return results;
		}
	}

	public Vector<Vector<Object>> query(String query) throws IOException {
		// Check for phrases
		query = " " + query + " ";
		String[] tokens = query.split("\"");
		String phrase;
		if(tokens.length == 3) {
			phrase = getPhrase(query);
			query = tokens[0] + tokens[2]; // Remove phrase from query
		} else {
			phrase = "";
		}
		
		Vector<Integer> parsed_query = getWords(query);

		Vector<Vector<Object>> results = search(parsed_query, phrase);


		return results;
	}


	public static void main(String[] args) {
		try {
			SearchEngine searchEngine = new SearchEngine();

            //check for phrase
            String inputString = String.join(" ",args);


            //check if query contains inverted commas
            boolean hasPhraseSearch = inputString.contains("\"") || inputString.contains("\'");
            System.out.println("Query is: " + inputString);
            System.out.println("Has phrase search? : " + hasPhraseSearch);

			
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}
}
