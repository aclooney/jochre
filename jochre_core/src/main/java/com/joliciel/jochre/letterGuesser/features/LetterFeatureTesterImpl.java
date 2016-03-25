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
package com.joliciel.jochre.letterGuesser.features;

import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.letterGuesser.LetterGuesserContext;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

class LetterFeatureTesterImpl implements LetterFeatureTester {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(LetterFeatureTesterImpl.class);
	private GraphicsService graphicsService;
	private BoundaryService boundaryService;
	private LetterGuesserService letterGuesserService;
	private FeatureService featureService;

	@Override
	public void applyFeatures(Set<LetterFeature<?>> features, Set<String> letters, int minImageId, int minShapeId) {	
		List<JochreImage> images = this.getGraphicsService().findImages(new ImageStatus[] {ImageStatus.TRAINING_VALIDATED});
		for (JochreImage image : images) {
			if (image.getId()>=minImageId) {
				this.testFeatures(image, features, letters, minShapeId);
			}
			image.clearMemory();
		}
	}	

	void testFeatures(JochreImage jochreImage, Set<LetterFeature<?>> features, Set<String> letters, int minShapeId) {
		for (Paragraph paragraph : jochreImage.getParagraphs()) {
			for (RowOfShapes row: paragraph.getRows()) {
				for (GroupOfShapes group : row.getGroups()) {// simply add this group's shapes
					ShapeSequence shapeSequence = boundaryService.getEmptyShapeSequence();
					for (Shape shape : group.getShapes())
						shapeSequence.addShape(shape);
					for (ShapeInSequence shapeInSequence : shapeSequence) {
						Shape shape = shapeInSequence.getShape();
						if (shape.getId()>=minShapeId&&(letters==null||letters.size()==0||letters.contains(shape.getLetter())))
							this.testFeatures(shapeInSequence, features);
					} // next shape
				} // next group
			} // next row
		} // next paragraph
	}

	void testFeatures(ShapeInSequence shapeInSequence, Set<LetterFeature<?>> features) {
		LetterSequence history = null;
		LetterGuesserContext context = this.letterGuesserService.getContext(shapeInSequence, history);
		for (LetterFeature<?> feature : features) {
			RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
			feature.check(context, env);
		}
	}


	public GraphicsService getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}

	public BoundaryService getBoundaryService() {
		return boundaryService;
	}

	public void setBoundaryService(BoundaryService boundaryService) {
		this.boundaryService = boundaryService;
	}

	public LetterGuesserService getLetterGuesserService() {
		return letterGuesserService;
	}

	public void setLetterGuesserService(LetterGuesserService letterGuesserService) {
		this.letterGuesserService = letterGuesserService;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

}