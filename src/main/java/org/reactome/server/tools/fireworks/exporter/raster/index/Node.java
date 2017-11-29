package org.reactome.server.tools.fireworks.exporter.raster.index;

import org.reactome.server.tools.diagram.data.fireworks.graph.FireworksNode;
import org.reactome.server.tools.fireworks.exporter.common.analysis.model.AnalysisType;
import org.reactome.server.tools.fireworks.exporter.common.profiles.ColorFactory;
import org.reactome.server.tools.fireworks.exporter.common.profiles.FireworksColorProfile;
import org.reactome.server.tools.fireworks.exporter.raster.layers.FireworksCanvas;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Node extends FireworksElement {

	static final double P_VALUE_THRESHOLD = 0.05;
	private static final double MIN_NODE_SIZE = 0.025;
	private static final int NODE_FACTOR = 18;
	private static final Stroke SELECTION_STROKE = new BasicStroke(0.3f);
	private static final Stroke FLAG_STROKE = new BasicStroke(0.7f);
	private FireworksNode node;

	private Set<Edge> parents;
	private List<Double> exp;

	Node(FireworksNode node) {
		this.node = node;
	}

	Edge addChild(Node child) {
		if (this == child) return null;
		final Edge edge = new Edge(this, child);
//		if (to == null) to = new HashSet<>();
//		to.add(edge);
		if (child.parents == null)
			child.parents = new HashSet<>();
		child.parents.add(edge);
		return edge;
	}

	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);
		// Fire parents selection
		if (parents != null)
			parents.forEach(edge -> edge.setSelected(selected));
	}

	@Override
	public void setFlag(boolean flag) {
		super.setFlag(flag);
		if (parents != null)
			parents.forEach(edge -> edge.setFlag(flag));
	}

	public FireworksNode getNode() {
		return node;
	}

	public void render(FireworksCanvas canvas, FireworksColorProfile profile, FireworksIndex index) {
		final double diameter = (node.getRatio() + MIN_NODE_SIZE) * NODE_FACTOR;
		final double x = node.getX() - diameter * 0.5;
		final double y = node.getY() - diameter * 0.5;
		final Shape ellipse = new Ellipse2D.Double(x, y, diameter, diameter);

		draw(canvas, profile, ellipse, index);
		selection(canvas, profile, ellipse);
		flag(canvas, profile, ellipse);
		text(canvas);
	}

	private void draw(FireworksCanvas canvas, FireworksColorProfile profile, Shape ellipse, FireworksIndex index) {
		final Color color = getNodeColor(profile, index);
		canvas.getNodes().add(ellipse, color);
	}

	private Color getNodeColor(FireworksColorProfile profile, FireworksIndex index) {
		if (index.getAnalysis().getResult() == null)
			return profile.getNode().getInitial();
		if (index.getAnalysis().getType() == AnalysisType.EXPRESSION) {
			if (exp != null) {
				if (getpValue() <= P_VALUE_THRESHOLD) {
					final double min = index.getAnalysis().getResult().getExpression().getMin();
					final double max = index.getAnalysis().getResult().getExpression().getMax();
					final double val = 1 - (exp.get(0) - min) / (max - min);
					return ColorFactory.interpolate(profile.getNode().getExpression(), val);
				}
				return profile.getNode().getHit();
			}
		} else if (index.getAnalysis().getType() == AnalysisType.OVERREPRESENTATION
				|| index.getAnalysis().getType() == AnalysisType.SPECIES_COMPARISON) {
			if (getpValue() != null && getpValue() <= P_VALUE_THRESHOLD) {
				final double val = getpValue() / P_VALUE_THRESHOLD;
				return ColorFactory.interpolate(profile.getNode().getEnrichment(), val);
			}
		}
		return profile.getNode().getFadeout();
	}

	private void selection(FireworksCanvas canvas, FireworksColorProfile profile, Shape ellipse) {
		if (isSelected())
			canvas.getNodeSelection().add(ellipse, profile.getNode().getSelection(), SELECTION_STROKE);
	}

	private void flag(FireworksCanvas canvas, FireworksColorProfile profile, Shape ellipse) {
		if (isFlag())
			canvas.getNodeFlags().add(ellipse, profile.getNode().getFlag(), FLAG_STROKE);
	}

	private void text(FireworksCanvas canvas) {
		if (parents == null) {
			final Color color = isSelected()
					? Color.BLUE
					: Color.BLACK;
			canvas.getText().add(node.getName(), new Point2D.Double(node.getX(), node.getY()), color);
		}
	}

	public void setpValue(Double pValue) {
		super.setpValue(pValue);
		if (parents != null)
			this.parents.forEach(edge -> edge.setpValue(pValue));
	}

	void setExp(List<Double> exp) {
		this.exp = exp;
	}

	List<Double> getExp() {
		return exp;
	}
}
