///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.jochre.doc;

import java.io.File;
import java.util.List;

import com.joliciel.jochre.JochreSession;

/**
 * Image manipulation service, including segmentation into rows and letters.
 * 
 * @author Assaf Urieli
 *
 */
public interface DocumentService {
	/**
	 * Create a new empty JochreDocument.
	 */
	public JochreDocument getEmptyJochreDocument(JochreSession jochreSession);

	/**
	 * Load a JochreDocument from the persistent store.
	 */
	public JochreDocument loadJochreDocument(int documentId);

	/**
	 * Load a JochreDocument by name.
	 */
	public JochreDocument loadJochreDocument(String name);

	/**
	 * Find all existing documents in the database.
	 */
	public List<JochreDocument> findDocuments();

	/**
	 * Find all existing authors in the database.
	 */
	public List<Author> findAuthors();

	public Author loadAuthor(int authorId);

	public Author getEmptyAuthor();

	/**
	 * Load a JochrePage from the persistent store.
	 */
	public JochrePage loadJochrePage(int pageId);

	public ImageDocumentExtractor getImageDocumentExtractor(File imageFile, SourceFileProcessor documentProcessor);

	public JochreDocumentGenerator getJochreDocumentGenerator(JochreDocument jochreDocument, JochreSession jochreSession);

	public JochreDocumentGenerator getJochreDocumentGenerator(String filename, String userFriendlyName, JochreSession jochreSession);
}
