package server.model.players.packets;

import server.Config;
import server.model.npcs.NPCHandler;
import server.model.players.Client;
import server.model.players.PacketType;

/**
 * Click NPC
 */
public class ClickNPC implements PacketType {
	public static final int ATTACK_NPC = 72, MAGE_NPC = 131, FIRST_CLICK = 155,
			SECOND_CLICK = 17, THIRD_CLICK = 21;

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		c.npcIndex = 0;
		c.npcClickIndex = 0;
		c.playerIndex = 0;
		c.clickNpcType = 0;
		c.getPA().resetFollow();
		switch (packetType) {

		/**
		 * Attack npc melee or range
		 **/
		case ATTACK_NPC:
			if (!c.mageAllowed) {
				c.mageAllowed = true;
				c.sendMessage("I can't reach that.");
				break;
			}
			c.npcIndex = c.getInStream().readUnsignedWordA();
			if (NPCHandler.npcs[c.npcIndex] == null) {
				c.npcIndex = 0;
				break;
			}
			if (NPCHandler.npcs[c.npcIndex].MaxHP == 0) {
				c.npcIndex = 0;
				break;
			}
			if (NPCHandler.npcs[c.npcIndex] == null) {
				break;
			}
			if (c.autocastId > 0)
				c.autocasting = true;
			if (!c.autocasting && c.spellId > 0) {
				c.spellId = 0;
			}
			c.faceUpdate(c.npcIndex);
			c.usingMagic = false;
			boolean usingBow = false;
			boolean usingOtherRangeWeapons = false;
			boolean usingArrows = false;
			boolean usingCross = c.playerEquipment[c.playerWeapon] == 9185;
			if (c.playerEquipment[c.playerWeapon] >= 4214
					&& c.playerEquipment[c.playerWeapon] <= 4223)
				usingBow = true;
			for (int bowId : c.BOWS) {
				if (c.playerEquipment[c.playerWeapon] == bowId) {
					usingBow = true;
					for (int arrowId : c.ARROWS) {
						if (c.playerEquipment[c.playerArrows] == arrowId) {
							usingArrows = true;
						}
					}
				}
			}
			for (int otherRangeId : c.OTHER_RANGE_WEAPONS) {
				if (c.playerEquipment[c.playerWeapon] == otherRangeId) {
					usingOtherRangeWeapons = true;
				}
			}
			if ((usingBow || c.autocasting)
					&& c.goodDistance(c.getX(), c.getY(),
							NPCHandler.npcs[c.npcIndex].getX(),
							NPCHandler.npcs[c.npcIndex].getY(), 7)) {
				c.stopMovement();
			}

			if (usingOtherRangeWeapons
					&& c.goodDistance(c.getX(), c.getY(),
							NPCHandler.npcs[c.npcIndex].getX(),
							NPCHandler.npcs[c.npcIndex].getY(), 4)) {
				c.stopMovement();
			}
			if (!usingCross && !usingArrows && usingBow
					&& c.playerEquipment[c.playerWeapon] < 4212
					&& c.playerEquipment[c.playerWeapon] > 4223 && !usingCross) {
				c.sendMessage("You have run out of arrows!");
				break;
			}
			if (c.getCombat().correctBowAndArrows() < c.playerEquipment[c.playerArrows]
					&& Config.CORRECT_ARROWS
					&& usingBow
					&& !c.getCombat().usingCrystalBow()
					&& c.playerEquipment[c.playerWeapon] != 9185) {
				c.sendMessage("You can't use "
						+ c.getItems()
								.getItemName(c.playerEquipment[c.playerArrows])
								.toLowerCase()
						+ "s with a "
						+ c.getItems()
								.getItemName(c.playerEquipment[c.playerWeapon])
								.toLowerCase() + ".");
				c.stopMovement();
				c.getCombat().resetPlayerAttack();
				return;
			}
			if (c.playerEquipment[c.playerWeapon] == 9185
					&& !c.getCombat().properBolts()) {
				c.sendMessage("You must use bolts with a crossbow.");
				c.stopMovement();
				c.getCombat().resetPlayerAttack();
				return;
			}

			if (c.followId > 0) {
				c.getPA().resetFollow();
			}
			if (c.attackTimer <= 0) {
				c.getCombat().attackNpc(c.npcIndex);
				c.attackTimer++;
			}

			break;

		/**
		 * Attack npc with magic
		 **/
		case MAGE_NPC:
			if (!c.mageAllowed) {
				c.mageAllowed = true;
				c.sendMessage("I can't reach that.");
				break;
			}
			// c.usingSpecial = false;
			// c.getItems().updateSpecialBar();

			c.npcIndex = c.getInStream().readSignedWordBigEndianA();
			int castingSpellId = c.getInStream().readSignedWordA();
			c.usingMagic = false;

			if (NPCHandler.npcs[c.npcIndex] == null) {
				break;
			}

			if (NPCHandler.npcs[c.npcIndex].MaxHP == 0
					|| NPCHandler.npcs[c.npcIndex].npcType == 944) {
				c.sendMessage("You can't attack this npc.");
				break;
			}

			for (int i = 0; i < c.MAGIC_SPELLS.length; i++) {
				if (castingSpellId == c.MAGIC_SPELLS[i][0]) {
					c.spellId = i;
					c.usingMagic = true;
					break;
				}
			}
			if (castingSpellId == 1171) { // crumble undead
				for (int npc : Config.UNDEAD_NPCS) {
					if (NPCHandler.npcs[c.npcIndex].npcType != npc) {
						c.sendMessage("You can only attack undead monsters with this spell.");
						c.usingMagic = false;
						c.stopMovement();
						break;
					}
				}
			}
			/*
			 * if(!c.getCombat().checkMagicReqs(c.spellId)) { c.stopMovement();
			 * break; }
			 */

			if (c.autocasting)
				c.autocasting = false;

			if (c.usingMagic) {
				if (c.goodDistance(c.getX(), c.getY(),
						NPCHandler.npcs[c.npcIndex].getX(),
						NPCHandler.npcs[c.npcIndex].getY(), 6)) {
					c.stopMovement();
				}
				if (c.attackTimer <= 0) {
					c.getCombat().attackNpc(c.npcIndex);
					c.attackTimer++;
				}
			}

			break;

		case FIRST_CLICK:
			c.npcClickIndex = c.inStream.readSignedWordBigEndian();
			c.npcType = NPCHandler.npcs[c.npcClickIndex].npcType;
			if (c.goodDistance(NPCHandler.npcs[c.npcClickIndex].getX(),
					NPCHandler.npcs[c.npcClickIndex].getY(), c.getX(),
					c.getY(), 1)) {
				c.turnPlayerTo(NPCHandler.npcs[c.npcClickIndex].getX(),
						NPCHandler.npcs[c.npcClickIndex].getY());
				NPCHandler.npcs[c.npcClickIndex].facePlayer(c.playerId);
				c.getActions().firstClickNpc(c.npcType);
			} else {
				c.clickNpcType = 1;
			}
			break;

		case SECOND_CLICK:
			c.npcClickIndex = c.inStream.readUnsignedWordBigEndianA();
			c.npcType = NPCHandler.npcs[c.npcClickIndex].npcType;
			if (c.goodDistance(NPCHandler.npcs[c.npcClickIndex].getX(),
					NPCHandler.npcs[c.npcClickIndex].getY(), c.getX(),
					c.getY(), 1)) {
				c.turnPlayerTo(NPCHandler.npcs[c.npcClickIndex].getX(),
						NPCHandler.npcs[c.npcClickIndex].getY());
				NPCHandler.npcs[c.npcClickIndex].facePlayer(c.playerId);
				c.getActions().secondClickNpc(c.npcType);
			} else {
				c.clickNpcType = 2;
			}
			break;

		case THIRD_CLICK:
			c.npcClickIndex = c.inStream.readSignedWord();
			c.npcType = NPCHandler.npcs[c.npcClickIndex].npcType;
			if (c.goodDistance(NPCHandler.npcs[c.npcClickIndex].getX(),
					NPCHandler.npcs[c.npcClickIndex].getY(), c.getX(),
					c.getY(), 1)) {
				c.turnPlayerTo(NPCHandler.npcs[c.npcClickIndex].getX(),
						NPCHandler.npcs[c.npcClickIndex].getY());
				NPCHandler.npcs[c.npcClickIndex].facePlayer(c.playerId);
				c.getActions().thirdClickNpc(c.npcType);
			} else {
				c.clickNpcType = 3;
			}
			break;
		}

	}
}
