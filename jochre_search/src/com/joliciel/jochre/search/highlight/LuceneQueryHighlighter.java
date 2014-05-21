///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.jochre.search.highlight;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;

import com.joliciel.jochre.search.JochreQuery;
import com.joliciel.talismane.utils.LogUtils;

class LuceneQueryHighlighter implements Highlighter {
	private static final Log LOG = LogFactory.getLog(LuceneQueryHighlighter.class);

	/** for rewriting: we don't want slow processing from MTQs */
	private static final IndexReader EMPTY_INDEXREADER = new MultiReader();

	JochreQuery jochreQuery;
	Query query;
	double docCountLog;
	Map<BytesRef,Double> termLogs;
	IndexSearcher indexSearcher;
	SortedSet<Term> queryTerms;
	List<Term> queryTermList;
	List<AtomicReaderContext> leaves;
	Set<BytesRef> terms;

	public LuceneQueryHighlighter(JochreQuery jochreQuery, IndexSearcher indexSearcher) {
		try {
			this.indexSearcher = indexSearcher;
			this.jochreQuery = jochreQuery;
			query = rewrite(jochreQuery.getLuceneQuery());
			queryTerms = new TreeSet<Term>();
			query.extractTerms(queryTerms);
			if (LOG.isTraceEnabled())
				queryTermList = new ArrayList<Term>(queryTerms);

			final IndexReader reader = indexSearcher.getIndexReader();
			docCountLog = Math.log(reader.numDocs());

			IndexReaderContext readerContext = reader.getContext();
			leaves = readerContext.leaves();
			
			// since the same terms might be contained in the query multiple times (e.g. once per field)
			// we only consider them once each by using a HashSet
			terms = new HashSet<BytesRef>();
			Map<BytesRef,Integer> termFreqs = new HashMap<BytesRef, Integer>();
			for(Term term : queryTerms) {
				terms.add(term.bytes());
				termFreqs.put(term.bytes(), 0);
			}

			termLogs = new HashMap<BytesRef, Double>();
			for (Term term : queryTerms) {
				int freq = termFreqs.get(term.bytes());
				freq += reader.docFreq(term);
				termFreqs.put(term.bytes(), freq);
			}
			for (BytesRef term : terms) {
				int freq = termFreqs.get(term);
				termLogs.put(term, Math.log(freq));
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	public Map<Integer, Set<HighlightTerm>> highlight(Set<Integer> docIds, Set<String> fields) {
		try {
			Map<Integer,Set<HighlightTerm>> termMap = new HashMap<Integer, Set<HighlightTerm>>();
			Map<Integer,Document> idToDocMap = new HashMap<Integer, Document>();

			Set<Integer> myLeaves = new HashSet<Integer>();
			for (int docId : docIds) {
				Document luceneDoc = indexSearcher.doc(docId);
				idToDocMap.put(docId, luceneDoc);
				termMap.put(docId, new TreeSet<HighlightTerm>());
				int leaf = ReaderUtil.subIndex(docId, leaves);
				myLeaves.add(leaf);
			}
				
			for (int leaf : myLeaves) {
				AtomicReaderContext subContext = leaves.get(leaf);
				AtomicReader atomicReader = subContext.reader();
				
				int fieldCounter = 0;
				for (String field : fields) {
					fieldCounter++;
					if (LOG.isTraceEnabled())
						LOG.trace("Field " + fieldCounter + ": " + field);
					
					Terms atomicReaderTerms = atomicReader.terms(field);
					if (atomicReaderTerms == null) {
						continue; // nothing to do
					}
					TermsEnum termsEnum = atomicReaderTerms.iterator(TermsEnum.EMPTY);
					
					int termCounter = 0;
					for (BytesRef term : terms) {
						termCounter++;
						if (LOG.isTraceEnabled())
							LOG.trace("Searching for term " + termCounter + ": " + term.utf8ToString() + " in field " + field);
						
						if (!termsEnum.seekExact(term)) {
							continue; // term not found
						}
	
						DocsAndPositionsEnum docPosEnum = termsEnum.docsAndPositions(null, null, DocsAndPositionsEnum.FLAG_OFFSETS);
						int docId = docPosEnum.nextDoc();
						while (docId!=DocsAndPositionsEnum.NO_MORE_DOCS) {
			                if (docIds.contains(docId)) {
			                	Document doc = idToDocMap.get(docId);
			                	Set<HighlightTerm> highlightTerms = termMap.get(docId);
				                //Retrieve the term frequency in the current document
				                int freq=docPosEnum.freq();
			                    if (LOG.isTraceEnabled()) {
			                    	String extId = doc.get("id");
			                    	String path = doc.get("path");
			                    	LOG.trace("Found " + freq + " matches for doc " + docId + ", extId: " + extId + ", path: " + path);
			                    }
				                
				                for(int i=0; i<freq; i++){
				                    int position=docPosEnum.nextPosition();
				                    int start=docPosEnum.startOffset();
				                    int end=docPosEnum.endOffset();
			
				                    if (LOG.isTraceEnabled())
				                    	LOG.trace("Found match " + position + " at docId " + docId + ", field " + field + " start=" + start + ", end=" + end);

				                    
									HighlightTerm highlightTerm = new HighlightTerm(docId, field, start, end);
									highlightTerm.setWeight(this.weigh(term));
									if (highlightTerm.getWeight()>0)
										highlightTerms.add(highlightTerm);
								}
			                }
			                docId = docPosEnum.nextDoc();
						}
					} // next term
				} // next field
			} // next index leaf to search

			return termMap;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	private double weigh(BytesRef term) {
		// TODO: is IDF the best formula?
		double termCountLog = termLogs.get(term);
		if (termCountLog==Double.NEGATIVE_INFINITY)
			return 0;
		double idf = docCountLog - termCountLog;
		return idf;
	}

	/** 
	 * we rewrite against an empty indexreader: as we don't want things like
	 * rangeQueries that don't summarize the document
	 */
	private static Query rewrite(Query original) {
		try {
			Query query = original;
			for (Query rewrittenQuery = query.rewrite(EMPTY_INDEXREADER); rewrittenQuery != query;
			rewrittenQuery = query.rewrite(EMPTY_INDEXREADER)) {
				query = rewrittenQuery;
			}
			return query;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
}
