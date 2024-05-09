<%@ page language="java" contentType="application/json; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ page import="jdbm.RecordManager" %>
<%@ page import="jdbm.RecordManagerFactory" %>
<%@ page import="jdbm.htree.HTree" %>
<%@ page import="jdbm.helper.FastIterator" %>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="IRUtilities.*" %>
<%@ page import="PROJECT.*"%>
<%@ page import="java.io.Serializable"%>
<%@ page import="java.util.Vector"%>

<%

    String dbPath = getServletContext().getRealPath("/WEB-INF/database/Database");
    RecordManager recman = RecordManagerFactory.createRecordManager(dbPath);
    long invertedindexid = recman.getNamedObject("invertedindex");
    HTree invertedindex = HTree.load(recman, invertedindexid);
    long forwardindexid = recman.getNamedObject("forwardindex");
    HTree forwardindex = HTree.load(recman, forwardindexid);
    long titleInvertedindexid = recman.getNamedObject("titleInvertedindex");
    HTree titleInvertedindex = HTree.load(recman, titleInvertedindexid);
    long titleForwardindexid = recman.getNamedObject("titleForwardindex");
    HTree titleForwardindex = HTree.load(recman, titleForwardindexid);
    long urlToIdid = recman.getNamedObject("urlToId");
    HTree urlToId = HTree.load(recman, urlToIdid);
    long idToUrlid = recman.getNamedObject("idToUrl");
    HTree idToUrl = HTree.load(recman, idToUrlid);
    long wordToIdid = recman.getNamedObject("wordToId");
    HTree wordToId = HTree.load(recman, wordToIdid);
    long idToWordid = recman.getNamedObject("idToWord");
    HTree idToWord = HTree.load(recman, idToWordid);
    long bigramid = recman.getNamedObject("bigram");
    HTree bigram = HTree.load(recman, bigramid);
    long trigramid = recman.getNamedObject("trigram");
    HTree trigram = HTree.load(recman, trigramid);
    getServletContext().setAttribute("invertedindex", invertedindex);
    getServletContext().setAttribute("forwardindex", forwardindex);
    getServletContext().setAttribute("titleInvertedindex", titleInvertedindex);
    getServletContext().setAttribute("titleForwardindex", titleForwardindex);
    getServletContext().setAttribute("urlToId", urlToId);
    getServletContext().setAttribute("idToUrl", idToUrl);
    getServletContext().setAttribute("wordToId", wordToId);
    getServletContext().setAttribute("idToWord", idToWord);
    getServletContext().setAttribute("bigram", bigram);
    getServletContext().setAttribute("trigram", trigram);
%>



<%! 
    boolean isPhraseSearch(String input) {
        return input.contains("\"");
    }
%>

<%!
    int getMaxTfDoc(int wordId, int docId) throws IOException {

    HTree docForwardIndex = (HTree) getServletContext().getAttribute("forwardindex");
    Vector<Posting> wordPostingVector = (Vector<Posting>) docForwardIndex.get(docId);

    int result = 1;
    for(Posting wordPosting : wordPostingVector) {
        if(wordPosting.freq > result) {
            result = wordPosting.freq;
        }
    }
    return result;
	}

%>

<%!
	    double getTermWeightDoc(int wordId, int docId, int TF, int N) throws IOException {
        HTree docInvertedIndex = (HTree) getServletContext().getAttribute("invertedindex");
        HTree docForwardIndex = (HTree) getServletContext().getAttribute("forwardindex");
		Vector<Posting> docPostingList = (Vector<Posting>) docInvertedIndex.get(wordId);
		Vector<Posting> wordPostingList = (Vector<Posting>) docForwardIndex.get(wordId);

		// Score is derived from (Term Frequency * IDF) / (Doc Max Term Frequency)
		int N_hasTerm = docPostingList.size();
		double IDF = Math.log(N/N_hasTerm) / Math.log(2);
		int TF_max = getMaxTfDoc(wordId, docId);

		double score = (TF * IDF) / (TF_max);

		return score;
	}

%>
<%!
	    double getTermWeightTitle(int wordId, int docId, int TF, int N) throws IOException {
        HTree titleInvertedIndex = (HTree) getServletContext().getAttribute("titleInvertedindex");
        HTree titleForwardIndex = (HTree) getServletContext().getAttribute("titleForwardindex");
		Vector<Posting> docPostingList = (Vector<Posting>) titleInvertedIndex.get(wordId);
		Vector<Posting> wordPostingList = (Vector<Posting>) titleForwardIndex.get(wordId);

		// Score is derived from (Term Frequency * IDF) / (Doc Max Term Frequency)
		int N_hasTerm = docPostingList.size();
		double IDF = Math.log(N/N_hasTerm) / Math.log(2);
		int TF_max = getMaxTfTitle(wordId, docId);

		double score = (TF * IDF) / (TF_max);

		return score;
	}

%>
<%!
	    double getDocMagnitude(int docId, int N) throws IOException {
	    HTree docForwardIndex = (HTree) getServletContext().getAttribute("forwardindex");	
        Vector<Posting> wordPostingVector = (Vector<Posting>) docForwardIndex.get(docId);
		double magnitude = 0;
		for(Posting wordPosting : wordPostingVector) {
			magnitude += Math.pow(getTermWeightDoc(wordPosting.id, docId, wordPosting.freq, N), 2);
		}
		magnitude = Math.sqrt(magnitude);
		return magnitude;
	}

%>
<%!
	    double getTitleMagnitude(int docId, int N) throws IOException {
        HTree titleForwardIndex = (HTree) getServletContext().getAttribute("titleForwardindex");
		Vector<Posting> wordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);
		double magnitude = 0;
		for(Posting wordPosting : wordPostingVector) {
			magnitude += Math.pow(getTermWeightTitle(wordPosting.id, docId, wordPosting.freq, N), 2);
		}
		magnitude = Math.sqrt(magnitude);
		return magnitude;
	}


%>

<%!
        int getMaxTfTitle(int wordId, int docId) throws IOException {

        HTree titleForwardIndex = (HTree) getServletContext().getAttribute("titleForwardindex");
		Vector<Posting> wordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);

		int result = 1;

		for(Posting wordPosting : wordPostingVector) {
			if(wordPosting.freq > result) {
				result = wordPosting.freq;
			}
		}

		return result;
	}

%>

<%
    //Here is the part used for StopStem
    String stopWord = getServletContext().getRealPath("/WEB-INF/stopwords.txt");
    HashSet<String> stopWords = new HashSet<String>();
    Porter porter = new Porter();
    BufferedReader in = new BufferedReader(new FileReader(stopWord));
    String line;
    while ((line = in.readLine()) != null) {
        stopWords.add(line);
    }
    in.close();
    getServletContext().setAttribute("stopWords",stopWords);
%>


<%! 
    String getPhrase(String query) throws IOException {
        Porter porter = new Porter();
		query = " " + query + " ";

		String[] tokens = query.split("\"");
		String phrase = "";

        //Retrieve the stopWords HashMap from the servlet context
        HashSet<String> stopWords = (HashSet<String>) getServletContext().getAttribute("stopWords");

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

%>

<%!
	Vector<Integer> getWords(String query) throws IOException {
		query = " " + query + " ";
		String[] tokens = query.split("\"");
		Vector<Integer> wordIds = new Vector<Integer> ();

		if(tokens.length == 3) {
			query = tokens[0] + tokens[2];
		}
		query = query.stripLeading().stripTrailing();
		String[] wordTokens = query.split(" ");
        HTree wordToId = (HTree) getServletContext().getAttribute("wordToId");
		Set<Integer> tempSet = new HashSet<>();
        Porter porter = new Porter(); 
        HashSet<String> stopWords = (HashSet<String>) getServletContext().getAttribute("stopWords");
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
%>
<%! 
    	Vector<Vector<Object>> search(Vector<Integer> query, String phrase) throws IOException {
		if(phrase == "") {
			HashMap<Integer, Double> consolidatedScores = new HashMap<>();
			// Title Constant for prioritizing title matches
			int TITLE_CONSTANT = 4;
			// Find the total number of terms
			int N_doc = 0;
			int N_title = 0;
            HTree docInvertedIndex = (HTree) getServletContext().getAttribute("invertedindex");
            HTree titleInvertedIndex = (HTree) getServletContext().getAttribute("titleInvertedindex");
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

            HTree bigram = (HTree) getServletContext().getAttribute("bigram");
            HTree trigram = (HTree) getServletContext().getAttribute("trigram");
			if(tokens.length == 2) {
				docList = (HashSet<Integer>) bigram.get(phrase);
			} else if(tokens.length == 3) {
				docList = (HashSet<Integer>) trigram.get(phrase);
			}

			if(docList == null) {
				Vector<Vector<Object>> results = new Vector<Vector<Object>>();
				return results;
			}

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

%>
<%!
	public Vector<Vector<Object>> query(String query) throws IOException {
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

%>


<%
    if (isPhraseSearch(request.getParameter("input"))) {
        out.print("Phrase Search initiated");
    }

    String input = request.getParameter("input").trim();
    
    out.println(getWords(input));

    // case to handle null or empty input
    if (input == null || input.isEmpty()) {
        // add the JSON output for empty input (sortedPages and pages)
        out.print("{\"sortedPages\":[],\"pages\":{}}");
        return;
    }
    // Extract n-grams from the list of words
    List<String> words = Arrays.asList(input.split("\\s+"));
    List<String> one_gram = new ArrayList<String>();
    List<String> two_gram = new ArrayList<String>();
    List<String> three_gram = new ArrayList<String>();

    for (int i=0; i < words.size(); i++) {
        String ngram = "";
        // 1-gram
        for (int j=0; j<1; j++) {
            String word = words.get(i+j).toLowerCase();
            if (stopWords.contains(word)) {
                ngram = "";
                break;
            }
            ngram += porter.stripAffixes(word) + " ";
        }
        if (ngram != ""){
            one_gram.add(ngram.trim());
        }
        // 2-gram
        ngram = "";
        if (i < words.size()-1){
            for (int j=0; j<2; j++) {
                String word = words.get(i+j).toLowerCase();
                if (stopWords.contains(word)) {
                    ngram = "";
                    break;
                }
                ngram += porter.stripAffixes(word) + " ";
            }
            if (ngram != ""){
                two_gram.add(ngram.trim());
            }
        }
        // 3-gram
        ngram = "";
        if (i < words.size()-2){
            for (int j=0; j<3; j++) {
                String word = words.get(i+j).toLowerCase();
                if (stopWords.contains(word)) {
                    ngram = "";
                    break;
                }
                ngram += porter.stripAffixes(word) + " ";
            }
            if (ngram != ""){
                three_gram.add(ngram.trim());
            }
        }
    };

    // create a list of all the n-grams
    List<String> ngrams = new ArrayList<String>();
    ngrams.addAll(one_gram);
    ngrams.addAll(two_gram);
    ngrams.addAll(three_gram);

    /*
    Posting posting = new Posting(1,1);
    out.println(posting);
    out.println(invertedindex);
    out.println(invertedindex.keys());

    SearchEngine se = new SearchEngine(
    new Porter(), 
    stopWords,
    urlToId,
    wordToId,
    idToWord,
    titleForwardindex,
    forwardindex,
    titleInvertedindex,
    invertedindex,
    bigram,
    trigram,
    recman,
    idToWordid);
    */
    
    out.println(query(input));
    //out.println(se.query("hi"));
%>


