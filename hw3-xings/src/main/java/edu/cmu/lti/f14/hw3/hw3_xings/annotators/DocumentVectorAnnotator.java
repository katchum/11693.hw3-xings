package edu.cmu.lti.f14.hw3.hw3_xings.annotators;

import java.util.*;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import edu.cmu.lti.f14.hw3.hw3_xings.utils.Utils;

import edu.cmu.lti.f14.hw3.hw3_xings.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_xings.typesystems.Token;


public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}

	/**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
	 * @param doc input text
	 * @return    a list of tokens.
	 */

	List<String> tokenize0(String doc) {
	  List<String> res = new ArrayList<String>();
	  
	  for (String s: doc.split("\\s+"))
	    res.add(s);
	  return res;
	}

	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		
		//TO DO: construct a vector of tokens and update the tokenList in CAS
    //TO DO: use tokenize0 from above 
		List<String> tokenNames = tokenize0(docText);
		Map<String, Token> tokenFreq = new HashMap<String, Token>();
		for (String tokenWord : tokenNames){
			if (!tokenFreq.containsKey(tokenWord)){
				Token token = new Token(jcas);
				token.setText(tokenWord);
				token.setFrequency(1);
				tokenFreq.put(tokenWord, token);
			}
			else{
				//update the token
				Token temp = tokenFreq.get(tokenWord);
				temp.setFrequency(temp.getFrequency()+1);
				tokenFreq.put(tokenWord, temp);
			}
		}
		//add the tokens to the jcas
		FSList tokenList = Utils.fromCollectionToFSList(jcas, new ArrayList<Token>(tokenFreq.values()));
		doc.setTokenList(tokenList);
//		Iterator<String> keyit = tokenFreq.keySet().iterator();
//		while(keyit.hasNext()){
//			tokenFreq.get(keyit.next()).addToIndexes();
//		}
	}

}
