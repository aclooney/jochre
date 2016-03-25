package com.joliciel.jochre.boundaries;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Rectangle;
import com.joliciel.jochre.graphics.RectangleImpl;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.GeometricMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.Solution;

class ShapeSequenceImpl extends ArrayList<ShapeInSequence> implements ShapeSequence, Comparable<ShapeSequence> {
	private static final long serialVersionUID = 8564092412152008511L;
	
	private double score = 0.0;
	private boolean scoreCalculated = false;
	private BoundaryServiceInternal boundaryServiceInternal;
	
	private List<Decision> decisions = new ArrayList<Decision>();
	private List<Solution> underlyingSolutions = new ArrayList<Solution>();
	@SuppressWarnings("rawtypes")
	private ScoringStrategy scoringStrategy = new GeometricMeanScoringStrategy();
	
	ShapeSequenceImpl() {
		super();
	}
	
	public ShapeSequenceImpl(int initialCapacity) {
		super(initialCapacity);
	}
	
	/**
	 * Create a shape sequence from an existing history,
	 * with space for one additional shape at the end.
	 */
	public ShapeSequenceImpl(ShapeSequence history) {
		super(history.size()+1);
		this.addAll(history);
		this.decisions.addAll(history.getDecisions());
	}
	
	/**
	 * Combine two sequences into one.
	 */
	public ShapeSequenceImpl(ShapeSequence sequence1, ShapeSequence sequence2) {
		super((sequence1==null ? 0 : sequence1.size()) + (sequence2==null ? 0 : sequence2.size()));
		
		if (sequence1!=null) {
			this.addAll(sequence1);
			this.decisions.addAll(sequence1.getDecisions());
		}
		if (sequence2!=null) {
			this.addAll(sequence2);
			this.decisions.addAll(sequence2.getDecisions());
		}
		
		int i = 0;
		for (ShapeInSequence shapeInSequence : this) {
			shapeInSequence.setIndex(i);
			i++;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public double getScore() {
		if (!scoreCalculated) {
			score = this.scoringStrategy.calculateScore(this);
			scoreCalculated = true;
		}
		return score;
	}

	@Override
	public int compareTo(ShapeSequence o) {
		if (this.equals(o))
			return 0;
		if (this.getScore()<o.getScore()) {
			return 1;
		} else if (this.getScore()>o.getScore()) {
			return -1;
		} else {
			return 1;
		}
	}

	ShapeInSequence addShapeInternal(Shape shape, Shape originalShape) {
		ShapeInSequence shapeInSequence = boundaryServiceInternal.getShapeInSequence(this, shape, this.size());
		this.add(shapeInSequence);
		if (shape.getGroup()==null)
			shape.setGroup(originalShape.getGroup());
		return shapeInSequence;
	}
	
	@Override
	public ShapeInSequence addShape(Shape shape) {
		ShapeInSequence shapeInSequence = this.addShapeInternal(shape, shape);
		shapeInSequence.getOriginalShapes().add(shape);
		return shapeInSequence;
	}

	public BoundaryServiceInternal getBoundaryServiceInternal() {
		return boundaryServiceInternal;
	}

	public void setBoundaryServiceInternal(
			BoundaryServiceInternal boundaryServiceInternal) {
		this.boundaryServiceInternal = boundaryServiceInternal;
	}

	@Override
	public ShapeInSequence addShape(Shape shape, Shape originalShape) {
		ShapeInSequence shapeInSequence = this.addShapeInternal(shape, originalShape);
		shapeInSequence.getOriginalShapes().add(originalShape);
		return shapeInSequence;
	}

	@Override
	public ShapeInSequence addShape(Shape shape, Shape[] originalShapes) {
		ShapeInSequence shapeInSequence = this.addShapeInternal(shape, originalShapes[0]);
		for (Shape originalShape : originalShapes)
			shapeInSequence.getOriginalShapes().add(originalShape);
		return shapeInSequence;
	}
	
	@Override
	public ShapeInSequence addShape(Shape shape, List<Shape> originalShapes) {
		ShapeInSequence shapeInSequence = this.addShapeInternal(shape, originalShapes.get(0));
		for (Shape originalShape : originalShapes)
			shapeInSequence.getOriginalShapes().add(originalShape);
		return shapeInSequence;
	}

	@Override
	public List<Decision> getDecisions() {
		return decisions;
	}

	@Override
	public List<Solution> getUnderlyingSolutions() {
		return underlyingSolutions;
	}

	@Override
	public void addDecision(Decision decision) {
		this.decisions.add(decision);
	}

	@SuppressWarnings("rawtypes")
	public ScoringStrategy getScoringStrategy() {
		return scoringStrategy;
	}

	public void setScoringStrategy(@SuppressWarnings("rawtypes") ScoringStrategy scoringStrategy) {
		this.scoringStrategy = scoringStrategy;
	}

	@Override
	public Rectangle getRectangleInGroup(GroupOfShapes group) {
		boolean haveShapes = false;
		RectangleImpl rectangle = new RectangleImpl(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
		for (ShapeInSequence shapeInSequence : this) {
			Shape shape = shapeInSequence.getShape();
			if (shape.getGroup().equals(group)) {
				haveShapes = true;
				if (shape.getLeft()<rectangle.getLeft()) rectangle.setLeft(shape.getLeft());
				if (shape.getTop()<rectangle.getTop()) rectangle.setTop(shape.getTop());
				if (shape.getRight()>rectangle.getRight()) rectangle.setRight(shape.getRight());
				if (shape.getBottom()>rectangle.getBottom()) rectangle.setBottom(shape.getBottom());
			}
		}
		if (!haveShapes)
			rectangle = null;
		return rectangle;
	}

}