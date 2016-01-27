package com.joliciel.jochre.search;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.apache.lucene.index.IndexWriter;

public interface JochreIndexDocument {
	
	/**
	 * The full string contents of the document.
	 * @return
	 */
	public String getContents();
	
	/**
	 * Save the document to the Lucene index.
	 * @param indexWriter
	 */
	public void save(IndexWriter indexWriter);
	
	/**
	 * Get the rectangle enclosing a particular text line.
	 * @param pageIndex
	 * @param textBlockIndex
	 * @param textLineIndex
	 * @return
	 */
	public Rectangle getRectangle(int pageIndex, int textBlockIndex, int textLineIndex);
	
	/**
	 * The index of the first page contained in this document.
	 * @return
	 */
	public int getStartPage();
	
	/**
	 * The index of the last page contained in this document.
	 * @return
	 */
	public int getEndPage();
	
	/**
	 * The document's name, used as a prefix for various files in the document's directory.
	 * @return
	 */
	public String getName();
	
	/**
	 * Get the image corresponding to a particular page index.
	 * @param pageIndex
	 * @return
	 */
	public BufferedImage getImage(int pageIndex);

	public int getEndIndex(int pageIndex, int textBlockIndex, int textLineIndex);

	public int getStartIndex(int pageIndex, int textBlockIndex, int textLineIndex);
}