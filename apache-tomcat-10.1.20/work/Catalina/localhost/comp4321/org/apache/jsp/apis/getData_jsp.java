/*
 * Generated by the Jasper component of Apache Tomcat
 * Version: Apache Tomcat/10.1.20
 * Generated at: 2024-05-11 18:44:46 UTC
 * Note: The last modified time of this file was set to
 *       the last modified time of the source file after
 *       generation to assist with modification tracking.
 */
package org.apache.jsp.apis;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.jsp.*;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import java.io.*;
import java.util.*;
import IRUtilities.*;
import PROJECT.*;
import java.io.Serializable;
import java.util.Vector;

public final class getData_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent,
                 org.apache.jasper.runtime.JspSourceImports,
                 org.apache.jasper.runtime.JspSourceDirectives {

 
    boolean isPhraseSearch(String input) {
        return input.contains("\"");
    }


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



	    double getDocMagnitude(int docId, int N) throws IOException {
	    HTree docForwardIndex = (HTree) getServletContext().getAttribute("forwardindex");	
        Vector<Posting> wordPostingVector = (Vector<Posting>) docForwardIndex.get(docId);
        //gets a Vector<Posting<wordid,frequency>>
		double magnitude = 0;
		for(Posting wordPosting : wordPostingVector) {
			magnitude += Math.pow(getTermWeightDoc(wordPosting.id, docId, wordPosting.freq, N), 2);
		}
		magnitude = Math.sqrt(magnitude);
		return magnitude;
	}



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
				if(stopWords.contains(phraseToken)) {
					phrase = "";
					continue;
				}
				phrase += porter.stripAffixes(phraseToken);
				phrase += " ";


			}
		}

		phrase = phrase.stripTrailing();

		return phrase;
	}



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

 
    	Vector<Vector<Object>> search(Vector<Integer> query, String phrase) throws IOException {
		if(phrase == "") {
			//This means that there is no phrase
			HashMap<Integer, Double> consolidatedScores = new HashMap<>(); //key is docid, score
			// Title Constant for prioritizing title matches
			int TITLE_CONSTANT = 4;
			// Find the total number of terms
			int N_doc = 0; 
			int N_title = 0;
            HTree forwardIndex = (HTree) getServletContext().getAttribute("forwardindex");
			FastIterator iterTerms = forwardIndex.keys();
			Integer tempId;
			//This is changed. Previously it was wrongly the total number of terms! but it should be the total number of documents!!!!
			while ((tempId = (Integer) iterTerms.next()) != null) {
				N_doc++;
			}
			N_title=N_doc;

			try {
				for(Integer wordId : query) {
					if(wordId == null) {
						continue;
					}
					HTree docInvertedIndex = (HTree) getServletContext().getAttribute("invertedindex");
					HTree titleInvertedIndex = (HTree) getServletContext().getAttribute("titleInvertedindex");
					Vector<Posting> docPostingVector = (Vector<Posting>) docInvertedIndex.get(wordId);
					Vector<Posting> titlePostingVector = (Vector<Posting>) titleInvertedIndex.get(wordId);

					if(docPostingVector != null) {
						for (Posting docPosting : docPostingVector) {
							int docId = docPosting.id;
							int freq = docPosting.freq;
							double termWeight = getTermWeightDoc(wordId, docId, freq, N_doc);
							double docMagnitude = getDocMagnitude(docId, N_doc);

							termWeight = termWeight / docMagnitude;
							//used for cosine similarity, in denominator
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
							//used for cosine similarity, in denominator

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

			for(Integer key : keys) { //Pair<Docid, Score>
				Vector pair = new Vector<>();
				pair.add(key);
				pair.add(consolidatedScores.get(key) / queryMagnitude);
				//complete denominator for cosine similarity
				results.add(pair);
			}
			System.out.println(results.size());
			Collections.sort(results, new Comparator<Vector<Object>>() {
				@Override
				//correct. Return in descending order 
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
			//There is a phrase search!
			String[] tokens = phrase.split(" ");

			HashSet<Integer> docList = new HashSet<>();

            HTree bigram = (HTree) getServletContext().getAttribute("bigram");
            HTree trigram = (HTree) getServletContext().getAttribute("trigram");
            HTree unigram = (HTree) getServletContext().getAttribute("unigram");
   //          if (tokens.length == 1) {
   //              docList = (HashSet<Integer>) unigram.get(phrase);
   //          } else if(tokens.length == 2) {
			// 	docList = (HashSet<Integer>) bigram.get(phrase);
			// } else if(tokens.length == 3) {
			// 	docList = (HashSet<Integer>) trigram.get(phrase);
			// }
			HashSet<Integer> unigramResult = (HashSet<Integer>)unigram.get(phrase);
			HashSet<Integer> bigramResult = (HashSet<Integer>)bigram.get(phrase);
			HashSet<Integer> trigramResult = (HashSet<Integer>)trigram.get(phrase);
			if(unigramResult!= null) {
				docList.addAll(unigramResult);
			}
			if(bigramResult!= null) {
				docList.addAll(bigramResult);
			}
			if(trigramResult!= null) {
				docList.addAll(trigramResult);
			}

			if(docList == null) {
				//if phrase does not exist, just return nothing
				Vector<Vector<Object>> results = new Vector<Vector<Object>>();
				return results;
			}

			Vector<Vector<Object>> results = search(query, "");
			//repeat query, but without the phrase now 
			//results is Vector<Vector<Integer,Double>>
			Vector<Vector<Object>> filteredResult = new Vector<>();
			for (Vector<Object> entry:results){
				Integer documentId = (Integer) entry.get(0);
				if (docList.contains(documentId)) {
					filteredResult.add(entry);
				}
			}
			return filteredResult;
		}
	}



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


  private static final jakarta.servlet.jsp.JspFactory _jspxFactory =
          jakarta.servlet.jsp.JspFactory.getDefaultFactory();

  private static java.util.Map<java.lang.String,java.lang.Long> _jspx_dependants;

  private static final java.util.Set<java.lang.String> _jspx_imports_packages;

  private static final java.util.Set<java.lang.String> _jspx_imports_classes;

  static {
    _jspx_imports_packages = new java.util.LinkedHashSet<>(7);
    _jspx_imports_packages.add("java.util");
    _jspx_imports_packages.add("PROJECT");
    _jspx_imports_packages.add("jakarta.servlet");
    _jspx_imports_packages.add("java.io");
    _jspx_imports_packages.add("IRUtilities");
    _jspx_imports_packages.add("jakarta.servlet.http");
    _jspx_imports_packages.add("jakarta.servlet.jsp");
    _jspx_imports_classes = new java.util.LinkedHashSet<>(6);
    _jspx_imports_classes.add("java.util.Vector");
    _jspx_imports_classes.add("jdbm.RecordManager");
    _jspx_imports_classes.add("jdbm.RecordManagerFactory");
    _jspx_imports_classes.add("jdbm.helper.FastIterator");
    _jspx_imports_classes.add("jdbm.htree.HTree");
    _jspx_imports_classes.add("java.io.Serializable");
  }

  private volatile jakarta.el.ExpressionFactory _el_expressionfactory;
  private volatile org.apache.tomcat.InstanceManager _jsp_instancemanager;

  public java.util.Map<java.lang.String,java.lang.Long> getDependants() {
    return _jspx_dependants;
  }

  public java.util.Set<java.lang.String> getPackageImports() {
    return _jspx_imports_packages;
  }

  public java.util.Set<java.lang.String> getClassImports() {
    return _jspx_imports_classes;
  }

  public boolean getErrorOnELNotFound() {
    return false;
  }

  public jakarta.el.ExpressionFactory _jsp_getExpressionFactory() {
    if (_el_expressionfactory == null) {
      synchronized (this) {
        if (_el_expressionfactory == null) {
          _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
        }
      }
    }
    return _el_expressionfactory;
  }

  public org.apache.tomcat.InstanceManager _jsp_getInstanceManager() {
    if (_jsp_instancemanager == null) {
      synchronized (this) {
        if (_jsp_instancemanager == null) {
          _jsp_instancemanager = org.apache.jasper.runtime.InstanceManagerFactory.getInstanceManager(getServletConfig());
        }
      }
    }
    return _jsp_instancemanager;
  }

  public void _jspInit() {
  }

  public void _jspDestroy() {
  }

  public void _jspService(final jakarta.servlet.http.HttpServletRequest request, final jakarta.servlet.http.HttpServletResponse response)
      throws java.io.IOException, jakarta.servlet.ServletException {

    if (!jakarta.servlet.DispatcherType.ERROR.equals(request.getDispatcherType())) {
      final java.lang.String _jspx_method = request.getMethod();
      if ("OPTIONS".equals(_jspx_method)) {
        response.setHeader("Allow","GET, HEAD, POST, OPTIONS");
        return;
      }
      if (!"GET".equals(_jspx_method) && !"POST".equals(_jspx_method) && !"HEAD".equals(_jspx_method)) {
        response.setHeader("Allow","GET, HEAD, POST, OPTIONS");
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "JSPs only permit GET, POST or HEAD. Jasper also permits OPTIONS");
        return;
      }
    }

    final jakarta.servlet.jsp.PageContext pageContext;
    jakarta.servlet.http.HttpSession session = null;
    final jakarta.servlet.ServletContext application;
    final jakarta.servlet.ServletConfig config;
    jakarta.servlet.jsp.JspWriter out = null;
    final java.lang.Object page = this;
    jakarta.servlet.jsp.JspWriter _jspx_out = null;
    jakarta.servlet.jsp.PageContext _jspx_page_context = null;


    try {
      response.setContentType("application/json; charset=UTF-8");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("\n");
      out.write("    \n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");


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

    long metadataid = recman.getNamedObject("metadata");
    HTree metadata = HTree.load(recman, metadataid);

    long idToWordid = recman.getNamedObject("idToWord");
    HTree idToWord = HTree.load(recman, idToWordid);
    long bigramid = recman.getNamedObject("bigram");
    HTree bigram = HTree.load(recman, bigramid);
    long trigramid = recman.getNamedObject("trigram");
    HTree trigram = HTree.load(recman, trigramid);
    
    long unigramid = recman.getNamedObject("unigram");
    HTree unigram = HTree.load(recman, unigramid);
    
    long parentlinksid = recman.getNamedObject("parentLinks");
    HTree parentlinks = HTree.load(recman, parentlinksid);
    
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
    getServletContext().setAttribute("unigram", unigram);
    getServletContext().setAttribute("parentlinks", parentlinks);
    getServletContext().setAttribute("metadata", metadata);

      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');

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

      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');

    if (isPhraseSearch(request.getParameter("input"))) {
        //out.println("Phrase Search initiated");
    }

    String input = request.getParameter("input").trim();
    

    // case to handle null or empty input
    if (input == null || input.isEmpty()) {
        // add the JSON output for empty input (sortedPages and pages)
        out.print("{\"sortedPages\":[],\"pages\":{}}");
        return;
    }
    //out.println(input);
    //out.println(getPhrase(input));
    //getPhrase works
   	
	Vector<Vector<Object>> results = query(input);

	StringBuilder sb = new StringBuilder();
	sb.append("{ \"results\": [");

	int counter = 0; // Initialize counter
	for (Vector<Object> result : results) {
		if (counter == 50) {
			break;
		}

		double score = (double) result.get(1);
		int docId = (int) result.get(0);
		HTree IdToUrl = (HTree) getServletContext().getAttribute("idToUrl");
		String url = (String) IdToUrl.get(docId);

		HTree Metadata = (HTree) getServletContext().getAttribute("metadata");
		Container data = (Container) Metadata.get(url);

		HTree Parentlinks = (HTree) getServletContext().getAttribute("parentlinks");
		Vector<String> curparentlinks = (Vector<String>)Parentlinks.get(url);

		Vector<String> childLinks = data.childLinks;
		int size = data.pageSize;
		long lastModified = data.lastModificationDate;
		Vector<String> title = data.title;

		HTree docForwardIndex = (HTree) getServletContext().getAttribute("forwardindex");
		Vector<Posting> wordFrequencies = (Vector<Posting>) docForwardIndex.get(docId);

		sb.append("{");
		sb.append("\"url\": \"" + url + "\",");
		sb.append("\"score\": " + score + ",");
		sb.append("\"size\": " + size + ",");
		sb.append("\"lastModified\": " + lastModified + ",");
		sb.append("\"title\": \"" + String.join(" ", title) + "\",");
		sb.append("\"childLinks\": [\"" + String.join("\",\"", childLinks) + "\"],");
		sb.append("\"parentLinks\": [\"" + String.join("\",\"", curparentlinks) + "\"],");
		sb.append("\"wordFrequencies\": [");

        HTree IdToWord = (HTree) getServletContext().getAttribute("idToWord");
		for (Posting word : wordFrequencies) {
			sb.append("{");
			sb.append("\"word\": \"" + IdToWord.get(word.id) + "\", ");
			sb.append("\"frequency\": " + word.freq);
			sb.append("},");
		}

		if (!wordFrequencies.isEmpty()) {
			sb.setLength(sb.length() - 1); // Remove the trailing comma
		}

		sb.append("]},");

		counter++;
	}

	if (counter > 0) {
		sb.setLength(sb.length() - 1); // Remove the trailing comma after the last result
	}

	sb.append("]}");

	String jsonResult = sb.toString();


    response.setCharacterEncoding("UTF-8");
    response.setContentType("text/html;charset=UTF-8");
    response.getWriter().write(jsonResult);
    

      out.write('\n');
      out.write('\n');
      out.write('\n');
    } catch (java.lang.Throwable t) {
      if (!(t instanceof jakarta.servlet.jsp.SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          try {
            if (response.isCommitted()) {
              out.flush();
            } else {
              out.clearBuffer();
            }
          } catch (java.io.IOException e) {}
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
        else throw new ServletException(t);
      }
    } finally {
      _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}
