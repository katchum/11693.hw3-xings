package edu.cmu.lti.f14.hw3.hw3_xings.casconsumers;

import edu.cmu.lti.f14.hw3.hw3_xings.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_xings.typesystems.Token;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_xings.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_xings.utils.Utils;


public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and text relevant values **/
	public ArrayList<Integer> relList;
	
	public Map<Integer, Map<String, Integer>> queryVectors;
	
	public Map<Integer, ArrayList<Map<String, Integer>>> docVectors;
	
	public ArrayList<String> textList;
	
	public ArrayList<Integer> rankList;
	
	private PrintWriter writer = null;
	
		
	public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();

		relList = new ArrayList<Integer>();
		
		textList = new ArrayList<String>();
		
		queryVectors = new TreeMap<Integer, Map<String, Integer>>();

		docVectors = new TreeMap<Integer, ArrayList<Map<String, Integer>>>();
		
		rankList = new ArrayList<Integer>();
		
		try {
			writer = new PrintWriter("report.txt", "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * TODO :: 1. construct the global word dictionary 2. keep the word
	 * frequency for each sentence
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
	
		if (it.hasNext()) {
			Document doc = (Document) it.next();

			//Make sure that your previous annotators have populated this in CAS
			FSList fsTokenList = doc.getTokenList();
			//ArrayList<Token>tokenList=Utils.fromFSListToCollection(fsTokenList, Token.class);

			qIdList.add(doc.getQueryID());
			relList.add(doc.getRelevanceValue());
			//Do something useful here
			textList.add(doc.getText());
			if (doc.getRelevanceValue() == 99){
				ArrayList<Token> queryTokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
				Iterator<Token> queryIt = queryTokenList.iterator();
				Map<String, Integer> queryVector = new TreeMap<String, Integer>();
				while(queryIt.hasNext()){
					Token token = queryIt.next();
					queryVector.put(token.getText(), token.getFrequency());
				}
				queryVectors.put(doc.getQueryID(), queryVector);
			}else{
				ArrayList<Token> docTokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
				Iterator<Token> docIt = docTokenList.iterator();
				Map<String, Integer> docVector = new TreeMap<String, Integer>();
				while(docIt.hasNext()){
					Token token = docIt.next();
					docVector.put(token.getText(), token.getFrequency());
				}
				if(docVectors.containsKey(doc.getQueryID())){
					ArrayList<Map<String, Integer>> temp = docVectors.get(doc.getQueryID());
					temp.add(docVector);
					docVectors.put(doc.getQueryID(), temp);
				}else{
					ArrayList<Map<String, Integer>> temp = new ArrayList<Map<String, Integer>>();
					temp.add(docVector);
					docVectors.put(doc.getQueryID(), temp);
				}
			}
		}

	}

	/**
	 * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2.
	 * Compute the MRR metric
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);
		
		//My Code Starts Here
		Iterator<Integer> qidIt = qIdList.iterator();
		Iterator<Integer> relIt = relList.iterator();
		Iterator<String> textIt = textList.iterator();
		DecimalFormat format = new DecimalFormat("0.0000");
//		Iterator<ArrayList<Map<String, Integer>>> docIt = docVectors.iterator();
		while(qidIt.hasNext()){
			int qId = qidIt.next();
			int rel = relIt.next();
			String text = textIt.next();
			if(rel != 99){	//reaches the first retrieved document
		// TODO :: compute the cosine similarity measure
				//compute all the cosine similarities once reaches the first retrieved document of the same qid
				ArrayList<Map<String, Integer>> docList = docVectors.get(qId);
				Iterator<Map<String, Integer>> docIt = docList.iterator();
				ArrayList<Double> cosine_similarities = new ArrayList<Double>();
				while(docIt.hasNext()){
					double cosine_similarity = computeCosineSimilarity(queryVectors.get(qId),docIt.next());
					cosine_similarities.add(cosine_similarity);
				}
		// TODO :: compute the rank of retrieved sentences
				int i = 0;
				Iterator<Double> temp = cosine_similarities.iterator();
//				while(temp.hasNext()){
//					System.out.println(temp.next());
//				}
//				System.out.println("");
				while(rel!=99){
					if (rel == 1){
						double cs = cosine_similarities.get(i);
						ArrayList<Double> sorted = new ArrayList<Double>();
						for(Double k : cosine_similarities){
							sorted.add(k);
						}
						Collections.sort(sorted);
						Collections.reverse(sorted);
						int rank = 999;
						for (int j = 0; j<sorted.size(); j++){
							if (cs == sorted.get(j)){
								rank = j+1;
								break;
							}
						}
						writer.println("cosine=" + format.format(cs) + "\trank=" + rank + "\tqid=" + qId + "\trel=" + rel + "\t" + text);
						rankList.add(rank);
					}
					i++;
					if(!qidIt.hasNext()){
						break;
					}
					rel = relIt.next();
					qId = qidIt.next();
					text = textIt.next();
				}
			}
		}
		// TODO :: compute the metric:: mean reciprocal rank
		double metric_mrr = compute_mrr();
		System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
		writer.println("MRR=" + format.format(metric_mrr));
		writer.close();
	}

	/**
	 * 
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector) {
		double cosine_similarity=0.0;

		// TODO :: compute cosine similarity between two sentences
		double queryNormalizer = 0.0, docNormalizer = 0.0;
		double numerator= 0.0;
		
		//calculate the numerator of cosine
		Iterator<String> it = queryVector.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			if (docVector.containsKey(key)){
				numerator += queryVector.get(key) * docVector.get(key);
			}
		}
		
		//calculate the denominator of cosine queryNormalizer + docNormalizer
		//calculate queryNormalizer
		Iterator<Integer> valueIt = queryVector.values().iterator();
		while(valueIt.hasNext()){
			queryNormalizer += Math.pow(valueIt.next(), 2);
		}
		queryNormalizer = Math.sqrt(queryNormalizer);
		//calculate docNormalizer
		valueIt = docVector.values().iterator();
		while(valueIt.hasNext()){
			docNormalizer += Math.pow(valueIt.next(), 2);
		}
		docNormalizer = Math.sqrt(docNormalizer);
		//calculate cosine similarity
		cosine_similarity = numerator/(queryNormalizer*docNormalizer);
		
		return cosine_similarity;
	}

	/**
	 * 
	 * @return mrr
	 */
	private double compute_mrr() {
		double metric_mrr=0.0;
		
		// TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
		// Assume correct input (only one correct document retrieved per query)
		double numerator = 0.0;
		Iterator<Integer> it = rankList.iterator();
		while(it.hasNext()){
			numerator = numerator + 1/((double)it.next());
		}
		metric_mrr = numerator/((double)rankList.size());
		
		return metric_mrr;
	}

}
