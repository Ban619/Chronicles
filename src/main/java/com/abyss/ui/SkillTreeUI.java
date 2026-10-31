package com.abyss.ui;

import com.abyss.entities.Player;
import com.abyss.skills.SkillNode;
import com.abyss.skills.SkillTree;
import com.abyss.utils.InputHandler;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * SkillTreeUI — Full-screen interactive skill tree panel.
 *
 * Layout:
 *   ┌─────────────────────────────────────────────────────────┐
 *   │  SKILL TREE                     [K] Close   SP: 3       │
 *   ├──────────────┬──────────────────┬───────────────────────┤
 *   │  WARRIOR ⚔   │    ARCANE ✦      │    SHADOW 👣          │
 *   │   (red)      │    (blue)        │    (green)            │
 *   │              │                  │                        │
 *   │  T0 ● ─────  │  ─────── ●       │  ● ───────            │
 *   │     |        │          |       │  |                    │
 *   │  T1 ●   ●   │     ●   ●        │  ●    ●              │
 *   │     ...      │     ...          │  ...                   │
 *   ├──────────────┴──────────────────┴───────────────────────┤
 *   │  [TOOLTIP] Node name — description. Cost: 2 SP          │
 *   └─────────────────────────────────────────────────────────┘
 *
 * Controls:
 *   Arrow keys — navigate between nodes
 *   Enter/Space — unlock / upgrade selected node
 *   K or Esc — close
 *
 * Visual states:
 *   ○ grey       — locked (prerequisites not met)
 *   ○ dark-tint  — unlockable (prerequisites met, have SP)
 *   ● branch-colour — unlocked, rank 1+
 *   ● gold        — max rank
 *   line between nodes — white if source unlocked, grey otherwise
 */
public class SkillTreeUI {

    private boolean visible = false;

    // ── Layout ────────────────────────────────────────────────────────────
    private static final int  NODE_R      = 22;   // node circle radius
    private static final int  TIER_HEIGHT = 90;   // vertical px between tiers
    private static final int  COL_WIDTH   = 100;  // horizontal px between columns

    // ── Selection ─────────────────────────────────────────────────────────
    private int  selectedCol = 0;
    private int  selectedRow = 0;
    private List<SkillNode> orderedNodes; // flattened in (col, tier) order for nav

    // ── Refs ──────────────────────────────────────────────────────────────
    private final Player    player;
    private final SkillTree tree;

    // ─────────────────────────────────────────────────────────────────────
    public SkillTreeUI(Player player, SkillTree tree) {
        this.player = player;
        this.tree   = tree;
        buildOrderedList();
    }

    private void buildOrderedList() {
        orderedNodes = new ArrayList<>(tree.getAllNodes());
        orderedNodes.sort((a, b) -> {
            int colCmp = Integer.compare(a.getCol(), b.getCol());
            return colCmp != 0 ? colCmp : Integer.compare(a.getTier(), b.getTier());
        });
    }

    // ── Toggle ────────────────────────────────────────────────────────────
    public void toggle() { visible = !visible; }
    public boolean isVisible() { return visible; }

    // ── Input ─────────────────────────────────────────────────────────────
    public void handleInput(InputHandler input) {
        if (!visible) return;

        if (input.isJustPressed("escape") || input.isJustPressed("k")) {
            visible = false;
            return;
        }

        // Navigation
        int maxCol = orderedNodes.stream().mapToInt(SkillNode::getCol).max().orElse(0);
        int maxTier = orderedNodes.stream().mapToInt(SkillNode::getTier).max().orElse(0);

        if (input.isJustPressed("right")) selectedCol = Math.min(maxCol, selectedCol + 1);
        if (input.isJustPressed("left"))  selectedCol = Math.max(0, selectedCol - 1);
        if (input.isJustPressed("down"))  selectedRow = Math.min(maxTier, selectedRow + 1);
        if (input.isJustPressed("up"))    selectedRow = Math.max(0, selectedRow - 1);

        // Clamp to nearest existing node in that column if gap
        if (input.isJustPressed("enter") || input.isJustPressed("space")) {
            SkillNode sel = getSelectedNode();
            if (sel != null) {
                boolean ok = tree.unlock(sel.getId());
                if (!ok) flashError();
            }
        }
    }

    private SkillNode getSelectedNode() {
        return orderedNodes.stream()
            .filter(n -> n.getCol() == selectedCol && n.getTier() == selectedRow)
            .findFirst().orElse(null);
    }

    private long errorFlash = 0;
    private void flashError() { errorFlash = System.currentTimeMillis(); }

    // ── Render ────────────────────────────────────────────────────────────
    public void render(GraphicsContext gc, double w, double h) {
        if (!visible) return;

        double panelW = Math.min(900, w - 40);
        double panelH = Math.min(600, h - 40);
        double px     = (w - panelW) / 2;
        double py     = (h - panelH) / 2;

        // ── Background ────────────────────────────────────────────────────
        gc.setFill(Color.color(0.04, 0.04, 0.10, 0.97));
        gc.fillRoundRect(px, py, panelW, panelH, 12, 12);
        gc.setStroke(Color.web("#c8a96e", 0.6));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(px, py, panelW, panelH, 12, 12);

        // ── Header ────────────────────────────────────────────────────────
        gc.setFill(Color.web("#c8a96e"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("SKILL TREE", px + 20, py + 30);

        gc.setFont(Font.font("Serif", 14));
        gc.setFill(Color.web("#a0ffa0"));
        gc.fillText("Skill Points: " + tree.getSkillPoints(), px + panelW - 180, py + 30);
        gc.setFill(Color.web("#808090"));
        gc.fillText("[↑↓←→] navigate   [Enter] unlock   [K] close", px + 20, py + 48);

        // Divider
        gc.setStroke(Color.web("#c8a96e", 0.3));
        gc.setLineWidth(1);
        gc.strokeLine(px + 10, py + 58, px + panelW - 10, py + 58);

        // ── Branch column headers ──────────────────────────────────────────
        double treeTop = py + 80;
        double[] branchX = computeBranchCentres(px, panelW);
        String[] branchLabels = { "⚔  WARRIOR", "✦  ARCANE", "👣  SHADOW" };
        Color[]  branchColors = { Color.web("#e74c3c"), Color.web("#3498db"), Color.web("#2ecc71") };

        for (int i = 0; i < 3; i++) {
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 13));
            gc.setFill(branchColors[i]);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(branchLabels[i], branchX[i], treeTop);
        }

        // ── Connector lines (drawn first, under nodes) ────────────────────
        renderConnectors(gc, px, treeTop, branchX);

        // ── Nodes ─────────────────────────────────────────────────────────
        SkillNode selected = getSelectedNode();
        SkillNode tooltip  = null;

        for (SkillNode node : orderedNodes) {
            double nx = nodeScreenX(node, branchX);
            double ny = nodeScreenY(node, treeTop);
            renderNode(gc, node, nx, ny, node == selected);
            if (node == selected) tooltip = node;
        }

        // ── Tooltip bar at bottom ─────────────────────────────────────────
        renderTooltip(gc, px, py, panelW, panelH, tooltip);

        // ── Error flash ───────────────────────────────────────────────────
        long sinceError = System.currentTimeMillis() - errorFlash;
        if (sinceError < 400) {
            double alpha = 0.35 * (1.0 - sinceError / 400.0);
            gc.setFill(Color.color(0.8, 0.1, 0.1, alpha));
            gc.fillRoundRect(px, py, panelW, panelH, 12, 12);
        }
    }

    // ── Node rendering ────────────────────────────────────────────────────
    private void renderNode(GraphicsContext gc, SkillNode node, double nx, double ny, boolean selected) {
        Color branchColor = Color.web(node.getBranchColorHex());
        boolean unlocked = node.isUnlocked();
        boolean maxed    = node.isMaxRank();
        boolean canUp    = node.canUpgrade(tree.getSkillPoints(), tree);

        // Outer glow for selected
        if (selected) {
            gc.setFill(Color.color(1, 1, 0.5, 0.15));
            gc.fillOval(nx - NODE_R - 6, ny - NODE_R - 6, (NODE_R + 6) * 2, (NODE_R + 6) * 2);
            gc.setStroke(Color.color(1, 1, 0.5, 0.9));
            gc.setLineWidth(2);
            gc.strokeOval(nx - NODE_R - 4, ny - NODE_R - 4, (NODE_R + 4) * 2, (NODE_R + 4) * 2);
        }

        // Node fill
        Color fill;
        if (maxed)          fill = Color.web("#f0c040");
        else if (unlocked)  fill = branchColor;
        else if (canUp)     fill = Color.color(branchColor.getRed() * 0.4,
                                               branchColor.getGreen() * 0.4,
                                               branchColor.getBlue() * 0.4, 0.9);
        else                fill = Color.color(0.15, 0.15, 0.18, 0.9);

        gc.setFill(fill);
        gc.fillOval(nx - NODE_R, ny - NODE_R, NODE_R * 2, NODE_R * 2);

        // Border
        Color border = unlocked ? branchColor.brighter() : Color.color(0.4, 0.4, 0.5);
        gc.setStroke(maxed ? Color.web("#f0c040") : border);
        gc.setLineWidth(unlocked ? 2.0 : 1.0);
        gc.strokeOval(nx - NODE_R, ny - NODE_R, NODE_R * 2, NODE_R * 2);

        // Icon
        gc.setFont(Font.font("Serif", 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(unlocked ? Color.WHITE : Color.color(0.5, 0.5, 0.6));
        gc.fillText(node.getIcon(), nx, ny + 6);

        // Rank pip dots under icon
        if (node.getMaxRank() > 1) {
            double pipY = ny + NODE_R + 8;
            double pipSpacing = 8;
            double pipStart = nx - (node.getMaxRank() - 1) * pipSpacing / 2.0;
            for (int r = 0; r < node.getMaxRank(); r++) {
                double px = pipStart + r * pipSpacing;
                gc.setFill(r < node.getCurrentRank() ? Color.web("#f0c040") : Color.color(0.3, 0.3, 0.35));
                gc.fillOval(px - 3, pipY - 3, 6, 6);
            }
        }

        // Cost badge (top-right of node)
        gc.setFont(Font.font("Serif", 9));
        gc.setFill(Color.color(0.9, 0.9, 0.5, 0.85));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(node.getCost() + "SP", nx + NODE_R - 2, ny - NODE_R + 10);
    }

    // ── Connector lines ───────────────────────────────────────────────────
    private void renderConnectors(GraphicsContext gc, double px, double treeTop, double[] branchX) {
        for (SkillNode node : orderedNodes) {
            for (String prereqId : node.getPrerequisites()) {
                SkillNode prereq = tree.getNode(prereqId);
                if (prereq == null) continue;

                double x1 = nodeScreenX(prereq, branchX);
                double y1 = nodeScreenY(prereq, treeTop) + NODE_R;
                double x2 = nodeScreenX(node, branchX);
                double y2 = nodeScreenY(node, treeTop) - NODE_R;

                boolean lit = prereq.isUnlocked();
                gc.setStroke(lit ? Color.color(1, 1, 1, 0.55) : Color.color(0.3, 0.3, 0.35, 0.7));
                gc.setLineWidth(lit ? 2 : 1);

                // Elbow connector: vertical then horizontal
                if (Math.abs(x1 - x2) < 5) {
                    gc.strokeLine(x1, y1, x2, y2);
                } else {
                    double midY = (y1 + y2) / 2.0;
                    gc.strokeLine(x1, y1, x1, midY);
                    gc.strokeLine(x1, midY, x2, midY);
                    gc.strokeLine(x2, midY, x2, y2);
                }
            }
        }
    }

    // ── Tooltip ───────────────────────────────────────────────────────────
    private void renderTooltip(GraphicsContext gc, double px, double py,
                                double panelW, double panelH, SkillNode node) {
        double ty = py + panelH - 72;
        gc.setStroke(Color.web("#c8a96e", 0.3));
        gc.setLineWidth(1);
        gc.strokeLine(px + 10, ty, px + panelW - 10, ty);

        if (node == null) {
            gc.setFill(Color.color(0.5, 0.5, 0.6));
            gc.setFont(Font.font("Serif", 13));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Select a node to view details", px + panelW / 2, ty + 30);
            return;
        }

        // Name + rank
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 15));
        gc.setFill(Color.web(node.getBranchColorHex()).brighter());
        gc.setTextAlign(TextAlignment.LEFT);
        String rankStr = node.getMaxRank() > 1 ? "  [Rank " + node.getCurrentRank() + "/" + node.getMaxRank() + "]" : "";
        gc.fillText(node.getIcon() + "  " + node.getName() + rankStr, px + 20, ty + 22);

        // Description
        gc.setFont(Font.font("Serif", 13));
        gc.setFill(Color.color(0.85, 0.85, 0.90));
        gc.fillText(node.getDescription(), px + 20, ty + 42);

        // Cost + status right side
        gc.setTextAlign(TextAlignment.RIGHT);
        String status;
        Color  statusColor;
        if (node.isMaxRank()) {
            status = "✓ MAX RANK"; statusColor = Color.web("#f0c040");
        } else if (tree.getSkillPoints() < node.getCost()) {
            status = "Need " + node.getCost() + " SP  (have " + tree.getSkillPoints() + ")";
            statusColor = Color.web("#e74c3c");
        } else if (!node.canUpgrade(tree.getSkillPoints(), tree)) {
            status = "Prerequisites not met"; statusColor = Color.web("#e67e22");
        } else {
            status = "Press [Enter] — costs " + node.getCost() + " SP";
            statusColor = Color.web("#2ecc71");
        }
        gc.setFill(statusColor);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        gc.fillText(status, px + panelW - 20, ty + 22);

        // Effect
        if (node.getEffect() != null) {
            gc.setFill(Color.color(0.7, 0.8, 1.0));
            gc.setFont(Font.font("Serif", 12));
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText("Effect: " + node.getEffect().getDescription(), px + panelW - 20, ty + 42);
        }
    }

    // ── Coordinate math ───────────────────────────────────────────────────
    private double[] computeBranchCentres(double px, double panelW) {
        // 3 branches: WARRIOR(cols 0-1), ARCANE(cols 2-3), SHADOW(cols 4-5)
        double third = panelW / 3.0;
        return new double[]{
            px + third * 0.5,   // WARRIOR centre
            px + third * 1.5,   // ARCANE centre
            px + third * 2.5    // SHADOW centre
        };
    }

    private double nodeScreenX(SkillNode node, double[] branchX) {
        // Map column index to X: cols 0-1 → branch 0, cols 2-3 → branch 1, cols 4-5 → branch 2
        int branchIdx = node.getBranch().ordinal();
        // Sub-column offset within branch (col 0 or 1 within each branch)
        int subCol    = node.getCol() - branchIdx * 2;
        double offset = (subCol == 0) ? -COL_WIDTH * 0.3 : COL_WIDTH * 0.3;
        return branchX[branchIdx] + offset;
    }

    private double nodeScreenY(SkillNode node, double treeTop) {
        return treeTop + 30 + node.getTier() * TIER_HEIGHT;
    }
}
