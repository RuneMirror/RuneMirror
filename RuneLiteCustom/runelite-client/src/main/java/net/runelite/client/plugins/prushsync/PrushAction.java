package net.runelite.client.plugins.prushsync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrushAction
{
	private int v;
	private long seq;
	private int tick;
	private PrushActionType type;

	private int param0;
	private int param1;
	private int opcode;
	private int identifier;
	private int itemId;
	private String option;
	private String target;

	private Integer worldX;
	private Integer worldY;
	private Integer worldPlane;

	// Relative offset from host player to destination (dest - player)
	private Integer relDx;
	private Integer relDy;

	// Host worldview base and player world position at the time of the click
	private Integer hostBaseX;
	private Integer hostBaseY;
	private Integer hostPlayerWorldX;
	private Integer hostPlayerWorldY;
	private Integer hostPlayerWorldPlane;
}
