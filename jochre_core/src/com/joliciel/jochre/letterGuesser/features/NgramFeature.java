package com.joliciel.jochre.letterGuesser.features;

import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.letterGuesser.Letter;
import com.joliciel.jochre.letterGuesser.LetterGuesserContext;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.StringFeature;

public class NgramFeature extends AbstractLetterFeature<String> implements StringFeature<LetterGuesserContext> {
	private static final Letter SPACE = new Letter(" ");
	private IntegerFeature<LetterGuesserContext> nFeature;
	
	public NgramFeature(IntegerFeature<LetterGuesserContext> nFeature) {
		this.nFeature = nFeature;
		this.setName(super.getName() + "(" + this.nFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<String> checkInternal(LetterGuesserContext context) {
		FeatureResult<String> result = null;
		
		FeatureResult<Integer> nResult = nFeature.check(context);
		if (nResult!=null) {
			int n = nResult.getOutcome();
			
			int historyToFind = n-1;
			String ngram = "";
			Shape shape = context.getShapeInSequence().getShape();
			LetterSequence history = context.getHistory();
			for (int i = 0; i < historyToFind; i++) {
				Letter letter = null;
				if (history!=null) {
					// this is during analysis, we look at the current history
					if (history.size()>i) {
						letter = history.get(history.size()-i-1);
					} else {
						letter = SPACE;
					}	
				} else {
					// this is during training - we look at the previous letters
					if (shape.getIndex()>i) {
						GroupOfShapes group = shape.getGroup();
						letter = new Letter(group.getShapes().get(shape.getIndex()-i-1).getLetter());
					} else {
						letter = SPACE;
					}
				}
				ngram = letter.getString() + ngram;
			}
	
			result = this.generateResult(ngram);
		}
		return result;
	}

}
