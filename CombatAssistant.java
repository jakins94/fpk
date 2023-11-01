package server.model.players;

import server.Config;
import server.Server;
import server.util.Misc;
import server.model.players.Client;
import server.model.players.PlayerSave;
import server.model.minigames.PestControl;
import server.model.npcs.NPCHandler;
import server.model.npcs.NPC;
import server.model.players.Player;
import server.event.*;
import server.clip.region.Region;
import server.world.*;

public class CombatAssistant{
public int strBonus;
public int attackLevel;
	private Client c;
	public CombatAssistant(Client Client) {
		this.c = Client;
	}
	
	public void stepAway() {
		if (Region.getClipping(c.getX() - 1, c.getY(), c.heightLevel, -1, 0)) {
		c.getPA().walkTo(-1, 0);
	         } else if (Region.getClipping(c.getX() + 1, c.getY(), c.heightLevel, 1, 0)) {
		c.getPA().walkTo(1, 0);
	        } else if (Region.getClipping(c.getX(), c.getY() - 1, c.heightLevel, 0, -1)) {
		c.getPA().walkTo(0, -1);
	        } else if (Region.getClipping(c.getX(), c.getY() + 1, c.heightLevel, 0, 1)) {
		c.getPA().walkTo(0, 1);
	        }
	}
	
	

	public void multiSpellEffectNPC(int npcId, int damage) {					
		switch(c.MAGIC_SPELLS[c.oldSpellId][0]) {
			case 12891:
			case 12881:
				if (Server.npcHandler.npcs[npcId].freezeTimer < -4) {
					Server.npcHandler.npcs[npcId].freezeTimer = getFreezeTime();
				}
			break;
		}	
	}

	public boolean checkMultiBarrageReqsNPC(int i) {
		if(Server.npcHandler.npcs[i] == null) {
			return false;
		} else {
			return true;
		}
	}

	public void appendMultiBarrageNPC(int npcId, boolean splashed, Client c) {
		if (Server.npcHandler.npcs[npcId] != null) {
			NPC n = (NPC)Server.npcHandler.npcs[npcId];
			if (n.isDead)
				return;
			if (n.heightLevel != c.heightLevel)
				return;
			if (checkMultiBarrageReqsNPC(npcId)) {
				c.barrageCount++;
				Server.npcHandler.npcs[npcId].underAttackBy = c.playerId;
				Server.npcHandler.npcs[npcId].underAttack = true;
				if (Misc.random(mageAtk()) > Misc.random(mageDef()) && !c.magicFailed) {
					if(getEndGfxHeight() == 100){ // end GFX
						n.gfx100(c.MAGIC_SPELLS[c.oldSpellId][5]);
					} else {
						n.gfx0(c.MAGIC_SPELLS[c.oldSpellId][5]);
					}
					int damage = Misc.random(c.MAGIC_SPELLS[c.oldSpellId][6]);
					if (Server.npcHandler.npcs[npcId].HP - damage < 0) {
						damage = Server.npcHandler.npcs[npcId].HP;
					}
					switch(c.MAGIC_SPELLS[c.oldSpellId][0]) {
						case 12919: // blood spells
						case 12929:
							int heal = (int)(damage / 4);
							if(c.playerLevel[3] + heal >= c.getPA().getLevelForXP(c.playerXP[3])) {
								c.playerLevel[3] = c.getPA().getLevelForXP(c.playerXP[3]);
							} else {
								c.playerLevel[3] += heal;
							}
							c.getPA().refreshSkill(3);
						break;
					}
					c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE), 6); 
					c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE/3), 3);
					Server.npcHandler.npcs[npcId].handleHitMask(damage);
					//Server.npcHandler.applyDamage(npcId);
					Server.npcHandler.npcs[npcId].hitDiff = damage;
					Server.npcHandler.npcs[npcId].HP -= damage;
					Server.npcHandler.npcs[npcId].hitUpdateRequired = true;
					Server.npcHandler.npcs[npcId].freezeTimer = getFreezeTime();
					Server.npcHandler.npcs[npcId].underAttackBy = c.playerId;
					Server.npcHandler.npcs[npcId].lastDamageTaken = System.currentTimeMillis();
					c.totalDamageDealt += damage;
					c.totalPlayerDamageDealt += damage;
					multiSpellEffectNPC(npcId, damage);
				} else {
					n.gfx100(85);
				}			
			}		
		}	
	}

	public void appendVengeanceNPC(int otherPlayer, int damage) {
		if (damage <= 0)
			return;
		if (c.npcIndex > 0 && Server.npcHandler.npcs[c.npcIndex] != null) {
			c.forcedText = "Taste Vengeance!";
			c.forcedChatUpdateRequired = true;
			c.updateRequired = true;
			c.vengOn = false;
			if ((Server.npcHandler.npcs[c.npcIndex].HP - damage) > 0) {
				damage = (int)(damage * 0.75);
				if (damage > Server.npcHandler.npcs[c.npcIndex].HP) {
					damage = Server.npcHandler.npcs[c.npcIndex].HP;
				}
				Server.npcHandler.npcs[c.npcIndex].HP -= damage;
				Server.npcHandler.npcs[c.npcIndex].hitDiff2 = damage;
				Server.npcHandler.npcs[c.npcIndex].hitUpdateRequired2 = true;
				Server.npcHandler.npcs[c.npcIndex].updateRequired = true;
			}
		}	
		c.updateRequired = true;
	}
	public int[][] slayerReqs = {{1648,1},{1612,15},{1643,45},{1637,52},{1618,50},{1624,65},{1610,75},{1613,80},{1615,85},{2783,90}};
	
	public boolean goodSlayer(int i) {
		for (int j = 0; j < slayerReqs.length; j++) {
			if (slayerReqs[j][0] == Server.npcHandler.npcs[i].npcType) {
				if (slayerReqs[j][1] > c.playerLevel[c.playerSlayer]) {
					c.sendMessage("You need a slayer level of " + slayerReqs[j][1] + " to harm this monster.");
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	* Attack Npcs
	*/
	public void attackNpc(int i) {
		
		if (Region.getClipping(c.getX() - 1, c.getY(), c.heightLevel, -1, 0)) {
			c.getPA().walkTo(-1, 0);
		} else if (Region.getClipping(c.getX() + 1, c.getY(), c.heightLevel, 1, 0)) {
			c.getPA().walkTo(1, 0);
		} else if (Region.getClipping(c.getX(), c.getY() - 1, c.heightLevel, 0, -1)) {
			c.getPA().walkTo(0, -1);
		} else if (Region.getClipping(c.getX(), c.getY() + 1, c.heightLevel, 0, 1)) {
			c.getPA().walkTo(0, 1);
		}

	
		if (Server.npcHandler.npcs[i] != null) {
			strBonus = c.playerBonus[10];
			calculateMeleeAttack();
			calculateRangeAttack();
			rangeMaxHit();
			boolean isBehindwall = false;
		int otherX = Server.npcHandler.npcs[i].getX();
		int otherY = Server.npcHandler.npcs[i].getY();
		
		if(otherX < c.getX())
		if(Region.blockedWest(c.getX(),c.getY(),c.heightLevel))
		isBehindwall = true;
		
		if(otherX > c.getX())
		if(Region.blockedEast(c.getX(),c.getY(),c.heightLevel))
		isBehindwall = true;
		
		if(otherY < c.getY())
		if(Region.blockedSouth(c.getX(),c.getY(),c.heightLevel))
		isBehindwall = true;
		
		if(otherY > c.getY())
		if(Region.blockedNorth(c.getX(),c.getY(),c.heightLevel))
		isBehindwall = true;
		
			if (Server.npcHandler.npcs[i].isDead || Server.npcHandler.npcs[i].MaxHP <= 0) {
				c.usingMagic = false;
				c.faceUpdate(0);
				c.npcIndex = 0;
				return;
			}
			if (Server.npcHandler.npcs[i].npcType == 110 && c.absY <= 10326) {
				c.usingMagic = false;
				c.faceUpdate(0);
				c.npcIndex = 0;
				return;
			}

			if(c.respawnTimer > 0) {
				c.npcIndex = 0;
				return;
			}
			if (Server.npcHandler.npcs[i].underAttackBy > 0 && Server.npcHandler.npcs[i].underAttackBy != c.playerId && !Server.npcHandler.npcs[i].inMulti() && (Server.npcHandler.npcs[i].npcType != 2455 && Server.npcHandler.npcs[i].npcType != 2456 && Server.npcHandler.npcs[i].npcType != 2554 && Server.npcHandler.npcs[i].npcType != 2555 && Server.npcHandler.npcs[i].npcType != 2556 && Server.npcHandler.npcs[i].npcType != 2557)) {
				if(!c.inBarrows2()){
				c.npcIndex = 0;
				c.sendMessage("This monster is already in combat.");
				return;
				}
			}
			if ((/*c.underAttackBy > 0 || */c.underAttackBy2 > 0) && c.underAttackBy2 != i && !c.inMulti() && (Server.npcHandler.npcs[i].npcType != 2455 && Server.npcHandler.npcs[i].npcType != 2456 && Server.npcHandler.npcs[i].npcType != 2554 && Server.npcHandler.npcs[i].npcType != 2555 && Server.npcHandler.npcs[i].npcType != 2556 && Server.npcHandler.npcs[i].npcType != 2557)) {
				resetPlayerAttack();
				c.sendMessage("I am already under attack.");
				return;
			}
			if (!goodSlayer(i)) {
				resetPlayerAttack();
				return;
			}
			if (Server.npcHandler.npcs[i].spawnedBy != c.playerId && Server.npcHandler.npcs[i].spawnedBy > 0) {
				resetPlayerAttack();
				c.sendMessage("This monster was not spawned for you.");
				return;
			}
			c.followId2 = i;
			//c.followId = 0;
			if(c.attackTimer <= 0) {
				boolean usingBow = false;
				boolean usingArrows = false;
				boolean usingOtherRangeWeapons = false;
				boolean usingCross = false;
				c.usingBow = false;
				
					if(c.playerEquipment[c.playerWeapon] == 9185 || c.playerEquipment[c.playerWeapon] == 15041)
						usingCross = true;
				c.bonusAttack = 0;
				c.rangeItemUsed = 0;
				c.projectileStage = 0;
				c.SaveGame();
				
				if(!c.usingMagic) {
					for (int bowId : c.BOWS) {
						if(c.playerEquipment[c.playerWeapon] == bowId) {
							usingBow = true;
							c.usingBow = true;
							for (int arrowId : c.ARROWS) {
								if(c.playerEquipment[c.playerArrows] == arrowId) {
									usingArrows = true;
								}
							}
						}
					}				
					
					if(c.playerEquipment[c.playerWeapon] == 13022){
						usingBow = true;
						c.usingBow = true;
					}
					
					for (int otherRangeId : c.OTHER_RANGE_WEAPONS) {
						if(c.playerEquipment[c.playerWeapon] == otherRangeId) {
							usingOtherRangeWeapons = true;
						}
					}
				}

				if(c.spellId > 0) {
					c.usingMagic = true;
				} else {

					if (c.autocasting && c.playerMagicBook != 2) {
						c.spellId = c.autocastId;
						c.usingMagic = true;
					}
					if(c.playerEquipment[c.playerWeapon] == 22494) {
						c.autocasting = true;
						c.spellId = 52;
						c.usingMagic = true;
						c.mageFollow = true;
					}
					if(c.playerEquipment[c.playerWeapon] == 19112) {
						c.autocasting = true;
						c.spellId = 53;
						c.usingMagic = true;
						c.mageFollow = true;
					}
					if(c.playerEquipment[c.playerWeapon] == 2415) {
						c.autocasting = true;
						c.spellId = 28;
						c.usingMagic = true;
						c.mageFollow = true;
					}
					if(c.playerEquipment[c.playerWeapon] == 2416) {
						c.autocasting = true;
						c.spellId = 29;
						c.usingMagic = true;
						c.mageFollow = true;
					}
					if(c.playerEquipment[c.playerWeapon] == 2417) {
						c.autocasting = true;
						c.spellId = 30;
						c.usingMagic = true;
						c.mageFollow = true;
					}

				}
				c.attackTimer = getAttackDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
				if(c.usingMagic)
					c.magicDamage = calculateMagicDamage(c, i);
				c.specAccuracy = 1.0;
				c.specDamage = 1.0;
				
				if (armaNpc(i) && !usingCross && !usingBow && !c.usingMagic && !usingCrystalBow() && !usingOtherRangeWeapons) {				
					resetPlayerAttack();
					return;
				}

				if(isBehindwall && !usingHally() && !usingCross && !usingBow && !c.usingMagic && !usingCrystalBow() && !usingOtherRangeWeapons)
					return;

				if((!c.goodDistance(c.getX(), c.getY(), Server.npcHandler.npcs[i].getX(), Server.npcHandler.npcs[i].getY(), 2) && (usingHally() && !usingOtherRangeWeapons && !usingBow && !c.usingMagic && !usingCrystalBow())) ||(!c.goodDistance(c.getX(), c.getY(), Server.npcHandler.npcs[i].getX(), Server.npcHandler.npcs[i].getY(), 4) && (usingOtherRangeWeapons && !usingBow && !c.usingMagic && !usingCrystalBow())) || (!c.goodDistance(c.getX(), c.getY(), Server.npcHandler.npcs[i].getX(), Server.npcHandler.npcs[i].getY(), 1) && (!usingOtherRangeWeapons && !usingHally() && !usingBow && !c.usingMagic && !usingCrystalBow())) || ((!c.goodDistance(c.getX(), c.getY(), Server.npcHandler.npcs[i].getX(), Server.npcHandler.npcs[i].getY(), 8) && (usingBow || c.usingMagic || usingCrystalBow())))) {
					c.attackTimer = 2;
					return;
				}
				
				if(!usingCross && !usingArrows && usingBow && (c.playerEquipment[c.playerWeapon] < 4212 || c.playerEquipment[c.playerWeapon] > 4223) && c.playerEquipment[c.playerWeapon] != 12926 && c.playerEquipment[c.playerWeapon] != 13022) {
					c.sendMessage("You have run out of arrows!");
					c.stopMovement();
					c.npcIndex = 0;
					return;
				} 
				if(correctBowAndArrows() < c.playerEquipment[c.playerArrows] && Config.CORRECT_ARROWS && usingBow && !usingCrystalBow() && c.playerEquipment[c.playerWeapon] != 9185 && c.playerEquipment[c.playerWeapon] != 15041) {
					c.sendMessage("You can't use "+c.getItems().getItemName(c.playerEquipment[c.playerArrows]).toLowerCase()+"s with a "+c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()+".");
					c.stopMovement();
					c.npcIndex = 0;
					return;
				}
				if ((c.playerEquipment[c.playerWeapon] == 9185 || c.playerEquipment[c.playerWeapon] == 15041) && !properBolts()) {
					c.sendMessage("You must use bolts with a crossbow.");
					c.stopMovement();
					resetPlayerAttack();
					return;				
				}	
				
				if(usingBow || c.usingMagic || c.autocasting || c.spellId > 0 || usingOtherRangeWeapons || usingCrystalBow() || (c.goodDistance(c.getX(), c.getY(), Server.npcHandler.npcs[i].getX(), Server.npcHandler.npcs[i].getY(), 2) && usingHally())) {
					c.stopMovement();
				}

				if(!checkMagicReqs(c.spellId)) {
					c.stopMovement();
					c.npcIndex = 0;
					return;
				}

				if(c.freezeTimer >= 0) {
					c.stopMovement();
					resetPlayerAttack();
					c.npcIndex = 0;
					return;
				}

				
				c.faceUpdate(i);
				//c.specAccuracy = 1.0;
				//c.specDamage = 1.0;
				Server.npcHandler.npcs[i].underAttackBy = c.playerId;
				Server.npcHandler.npcs[i].lastDamageTaken = System.currentTimeMillis();
				if(c.usingSpecial && !c.usingMagic && c.playerEquipment[c.playerWeapon] != 15042) {
					if(checkSpecAmount(c.playerEquipment[c.playerWeapon])) {
						c.lastWeaponUsed = c.playerEquipment[c.playerWeapon];
						c.lastArrowUsed = c.playerEquipment[c.playerArrows];
						activateSpecial(c.playerEquipment[c.playerWeapon], i);
						return;
					} else {
						c.sendMessage("You don't have the required special energy to use this attack.");
						c.usingSpecial = false;
						c.getItems().updateSpecialBar();
						c.npcIndex = 0;
						return;
					}
				}

				c.specMaxHitIncrease = 0;

				if(!c.usingMagic) {
					c.startAnimation(getWepAnim(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()));
				} else {
					c.startAnimation(c.MAGIC_SPELLS[c.spellId][2]);
				}

				c.lastWeaponUsed = c.playerEquipment[c.playerWeapon];
				c.lastArrowUsed = c.playerEquipment[c.playerArrows];
				if(!usingBow && !c.usingMagic && (!c.usingSpecial || c.playerEquipment[c.playerWeapon] == 15042) && !usingOtherRangeWeapons) { // melee hit delay
					c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
					c.projectileStage = 0;
					c.oldNpcIndex = i;
				}
				
				if((usingBow || usingCross) && !usingOtherRangeWeapons && !c.usingMagic) { // range hit delay					
					if (usingCross)
						c.usingBow = true;
					if (c.fightMode == 2)
						c.attackTimer--;
					c.lastArrowUsed = c.playerEquipment[c.playerArrows];
					c.lastWeaponUsed = c.playerEquipment[c.playerWeapon];
					c.gfx100(getRangeStartGFX());	
					c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
					c.projectileStage = 1;
					c.oldNpcIndex = i;
					if(c.playerEquipment[c.playerWeapon] >= 4212 && c.playerEquipment[c.playerWeapon] <= 4223) {
						c.rangeItemUsed = c.playerEquipment[c.playerWeapon];
						c.crystalBowArrowCount++;
						c.lastArrowUsed = 0;
					} else if(c.playerEquipment[c.playerWeapon] == 12926) {
						c.rangeItemUsed = c.playerEquipment[c.playerWeapon];
						c.lastArrowUsed = 0;
					} else {
						c.rangeItemUsed = c.playerEquipment[c.playerArrows];
						c.getItems().deleteArrow();	
					}
					fireProjectileNpc();
				}
							
				
				if(usingOtherRangeWeapons && !c.usingMagic && !usingBow && !usingCross) {	// knives, darts, etc hit delay		
					c.rangeItemUsed = c.playerEquipment[c.playerWeapon];
					c.getItems().deleteEquipment();
					c.gfx100(getRangeStartGFX());
					c.lastArrowUsed = 0;
					c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
					c.projectileStage = 1;
					c.oldNpcIndex = i;
					if (c.fightMode == 2)
						c.attackTimer--;
					fireProjectileNpc();	
				}

				if(c.usingMagic) {	// magic hit delay
				
					int pX = c.getX();
					int pY = c.getY();
					int nX = Server.npcHandler.npcs[i].getX();
					int nY = Server.npcHandler.npcs[i].getY();
					int offX = (pY - nY)* -1;
					int offY = (pX - nX)* -1;
					c.castingMagic = true;
					c.projectileStage = 2;
					if(c.MAGIC_SPELLS[c.spellId][3] > 0) {
						if(getStartGfxHeight() == 100) {
							c.gfx100(c.MAGIC_SPELLS[c.spellId][3]);
						} else {
							c.gfx0(c.MAGIC_SPELLS[c.spellId][3]);
						}
					}
					if(c.MAGIC_SPELLS[c.spellId][4] > 0) {
						c.getPA().createPlayersProjectile(pX, pY, offX, offY, 50, 78, c.MAGIC_SPELLS[c.spellId][4], getStartHeight(), getEndHeight(), i + 1, 50);
					}
					c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
					c.oldNpcIndex = i;
					c.oldSpellId = c.spellId;
                    c.spellId = 0;
					if (!c.autocasting)
						c.npcIndex = 0;
				}

				if(usingBow && Config.CRYSTAL_BOW_DEGRADES) { // crystal bow degrading
					if(c.playerEquipment[c.playerWeapon] == 4212) { // new crystal bow becomes full bow on the first shot
						c.getItems().wearItem(4214, 1, 3);
					}
					
					if(c.crystalBowArrowCount >= 250){
						switch(c.playerEquipment[c.playerWeapon]) {
							
							case 4223: // 1/10 bow
							c.getItems().wearItem(-1, 1, 3);
							c.sendMessage("Your crystal bow has fully degraded.");
							if(!c.getItems().addItem(4207, 1)) {
								Server.itemHandler.createGroundItem(c, 4207, c.getX(), c.getY(), 1, c.getId());
							}
							c.crystalBowArrowCount = 0;
							break;
							
							default:
							c.getItems().wearItem(++c.playerEquipment[c.playerWeapon], 1, 3);
							c.sendMessage("Your crystal bow degrades.");
							c.crystalBowArrowCount = 0;
							break;
							
						
						}
					}	
				}
			}
		}
	}
	

	public void delayedHit(int i) { // npc hit delay
		if (Server.npcHandler.npcs[i] != null) {
			if (Server.npcHandler.npcs[i].isDead) {
				c.npcIndex = 0;
				return;
			}
			Server.npcHandler.npcs[i].facePlayer(c.playerId);
			
			if (Server.npcHandler.npcs[i].underAttackBy > 0 && Server.npcHandler.getsPulled(i)) {
				Server.npcHandler.npcs[i].killerId = c.playerId;			
			} else if (Server.npcHandler.npcs[i].underAttackBy < 0 && !Server.npcHandler.getsPulled(i)) {
				Server.npcHandler.npcs[i].killerId = c.playerId;
			}
			c.lastNpcAttacked = i;
			//double slayerBonus = 1+((double)((double)c.playerLevel[18] / 99));//getback2
			double slayerBonus = 1.0;
			if(c.getPand().inMission())	slayerBonus = 1.0;
			if(c.projectileStage == 0 && !c.usingMagic && !c.castingMagic) { // melee hit damage
			
			
				int damageOnNPC = (int)(Misc.random(calculateMeleeMaxHit()));
				
				if(Server.npcHandler.dagColor != 0 && Server.npcHandler.npcs[i].npcType == 3498){
					damageOnNPC = 0;
					c.sendMessage("The dagannoth is currently resistant to that attack!");
				}
				if(Server.npcHandler.dagColor2 != 0 && Server.npcHandler.npcs[i].npcType == 1351){
					damageOnNPC = 0;
					c.sendMessage("The dagannoth is currently resistant to that attack!");
				}
				if(Server.npcHandler.npcs[i].npcType == 2554) {
					if(Server.npcHandler.kMinionsDead < 3) {
							damageOnNPC = 0;
							c.sendMessage("@red@K'ril Tsutsaroth's defences remain inpenetrable while his minions stand!");
						} else if(Server.npcHandler.krilWeak != 0) {
							damageOnNPC = 0;
							c.sendMessage("@red@K'ril Tsutsaroth's defences can only be penetrated by ranged attacks now!");
					}
				}

				if(c.playerEquipment[c.playerWeapon] == 11716 && (Server.npcHandler.npcs[i].npcType == 2554 || Server.npcHandler.npcs[i].npcType == 2555 || Server.npcHandler.npcs[i].npcType == 2556 || Server.npcHandler.npcs[i].npcType == 2557)) {
					if(Misc.random(2) == 1)
						damageOnNPC = (int)(damageOnNPC * 2);
					else
						damageOnNPC = (int)(damageOnNPC * 1.25);
				}

				if(damageOnNPC > 0 && c.playerEquipment[c.playerWeapon] == 15042 && Misc.random(10) <= 1) {
					c.playerLevel[3] += (int)(Math.round(damageOnNPC * .15) + 1);
					if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]))
						c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
					c.updateRequired = true;
					c.getPA().refreshSkill(3);
					Server.npcHandler.npcs[i].gfx0(754);
				}

				c.clawDamage[0] = (int)Math.round(damageOnNPC / 2);
				c.clawDamage[1] = (int)Math.round(damageOnNPC / 2);
				c.clawDamage[2] = (int)Math.round(damageOnNPC / 4);
				applyNpcMeleeDamage2(i, 1, damageOnNPC);
				if(c.doubleHit && !c.usingClaws) {
					if(c.playerEquipment[c.playerWeapon] == 13047 || c.playerEquipment[c.playerWeapon] == 19000) {
						if(c.abbyDagger == 1) {
							int theDamage = Misc.random(calculateMeleeMaxHit())+1;
							applyNpcMeleeDamage2(i, 2, theDamage);
							c.abbyDagger = 0;
						} else {
							applyNpcMeleeDamage2(i, 2, 0);
						}
					} else {
						applyNpcMeleeDamage2(i, 2, Misc.random(calculateMeleeMaxHit()));
					}
				}
				if(c.doubleHit && c.usingClaws) {
					applyNpcMeleeDamage2(i, 2, c.clawDamage[0]);
					c.clawNPCDelay = 2;
					c.usingClaws = false;
					
					CycleEventHandler.getSingleton().addEvent(c, new CycleEvent() {
						@Override
						public void execute(CycleEventContainer container) {
							if(c.clawNPCDelay > 0) {
								c.clawNPCDelay--;
								if (c.clawNPCDelay == 0) {
									c.getCombat().applyNpcMeleeDamage2(c.lastNpcAttacked, 2, c.clawDamage[1]);
									c.getCombat().applyNpcMeleeDamage2(c.lastNpcAttacked, 2, c.clawDamage[2]);
								}
							}
						}

						@Override
						public void stop() {

						}
					}, 1);
				}
			}

			if(!c.castingMagic && c.projectileStage > 0) { // range hit damage
				int damage = (int)((double)((double)Misc.random(rangeMaxHit())*(slayerBonus)));
			    if(Server.npcHandler.npcs[i].npcType == 3847)
			    {
			    	if(damage >= Server.npcHandler.npcs[i].HP && Server.npcHandler.canKillQueen == false) {
			    		damage = Server.npcHandler.npcs[i].HP - 1;
			    	}
			    }
				int damage2 = -1;
				
				
				
				if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1 || c.cannonSpec == 1)
					damage2 = Misc.random(rangeMaxHit())*(1+(c.playerLevel[18] / 99));
					
				boolean ignoreDef = false;
				if (Misc.random(5) == 1 && c.lastArrowUsed == 9243 && (c.playerEquipment[c.playerWeapon] == 9185 || c.playerEquipment[c.playerWeapon] == 15041)) {
					ignoreDef = true;
					Server.npcHandler.npcs[i].gfx0(758);
				}

				if(Server.npcHandler.dagColor != 2 && Server.npcHandler.npcs[i].npcType == 3498){
					damage = 0;
					damage2 = -1;
					c.sendMessage("The dagannoth is currently resistant to that attack!");
				}
				if(Server.npcHandler.dagColor2 != 2 && Server.npcHandler.npcs[i].npcType == 1351){
					damage = 0;
					damage2 = -1;
					c.sendMessage("The dagannoth is currently resistant to that attack!");
				}

				if(Server.npcHandler.npcs[i].npcType == 2554) {
					boolean canAttack = true;
					if(Server.npcHandler.krilWeak == 0 && (c.onLedge() || c.onLedge2() || c.onLedge3()))
						canAttack = false;
					else if(Server.npcHandler.kMinionsDead < 3) {
						damage = 0;
						damage2 = -1;
						c.sendMessage("@red@K'ril Tsutsaroth's defences remain inpenetrable while his minions stand!");
					} else if(Server.npcHandler.krilWeak != 0) {
						if(Server.npcHandler.krilWeak == 1 && !c.onLedge())
							canAttack = false;
						else if(Server.npcHandler.krilWeak == 3 && !c.onLedge3())
							canAttack = false;
						else if(Server.npcHandler.krilWeak == 2) {
							damage = 0;
							damage2 = -1;

							if(c.onLedge2())
								c.sendMessage("@red@K'ril Tsutsaroth's chest can only be effected by magic attacks.");
							else
								canAttack = false;
						}
					}

					if(!canAttack) {
							damage = 0;
							damage2 = -1;
							c.sendMessage("@red@K'ril Tsutsaroth is invulnerable from this point, try attacking from a different height!");
					}
				}
				
				if(Misc.random(Server.npcHandler.npcs[i].defence) > Misc.random(10+calculateRangeAttack()) && !ignoreDef) {
					damage = 0;
				} else if (Server.npcHandler.npcs[i].npcType == 2881 || Server.npcHandler.npcs[i].npcType == 2883 && !ignoreDef) {
					damage = 0;
				}
				
				if (Misc.random(4) == 1 && c.lastArrowUsed == 9242 && damage > 0 && !usingCrystalBow()) {
					Server.npcHandler.npcs[i].gfx0(754);
					damage = Server.npcHandler.npcs[i].HP/5;
					if(damage >= 250)
						damage = 250;
					c.handleHitMask(c.playerLevel[3]/10);
					c.dealDamage(c.playerLevel[3]/10);
					c.gfx0(754);					
				}
				
				if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1) {
					if (Misc.random(Server.npcHandler.npcs[i].defence) > Misc.random(10+calculateRangeAttack()))
						damage2 = 0;
				}
				if (c.dbowSpec) {
					Server.npcHandler.npcs[i].gfx100(1100);
					if (damage < 8)
						damage = 8;
					if (damage2 < 8)
						damage2 = 8;
					c.dbowSpec = false;
				}

				if (damage > 0 && Misc.random(5) == 1 && c.lastArrowUsed == 9244 && (c.playerEquipment[c.playerWeapon] == 9185 || c.playerEquipment[c.playerWeapon] == 15041)) {
					damage *= 1.65;
					Server.npcHandler.npcs[i].gfx0(756);
				}

				if (damage > 0 && c.lastArrowUsed == 9245 && ((c.playerEquipment[c.playerWeapon] == 9185 && Misc.random(6) == 1) || (c.playerEquipment[c.playerWeapon] == 15041 && Misc.random(5) == 1))) {
					if(c.lastWeaponUsed == 4214)
						return;
					int greatDamage = (int) (damage *= 1.25);
					int hpHeal = (int) (greatDamage * 0.25);
					 if (c.playerLevel[3] <= 99) {
						if (c.playerLevel[3] + hpHeal >= c.getPA().getLevelForXP(
								c.playerXP[3])) {
							c.playerLevel[3] = c.getPA().getLevelForXP(
									c.playerXP[3]);
									c.getPA().refreshSkill(3);
									c.sendMessage("Your Onyx bolts (e) heal you for a portion of the damage dealt.");
						}
					}
					NPCHandler.npcs[i].gfx0(753);
				}
				
				if (Server.npcHandler.npcs[i].HP - damage < 0) { 
					damage = Server.npcHandler.npcs[i].HP;
				}
				if (Server.npcHandler.npcs[i].HP - damage <= 0 && damage2 > 0) {
					damage2 = 0;
				}
				if(c.fightMode == 3) {
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 4); 
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 1);				
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 3);
					c.getPA().refreshSkill(1);
					c.getPA().refreshSkill(3);
					c.getPA().refreshSkill(4);
				} else {
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE), 4); 
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 3);
					c.getPA().refreshSkill(3);
					c.getPA().refreshSkill(4);
				}
				if (damage > 0 && PestControl.isInGame(c)) {
								if (NPCHandler.npcs[i].npcType == 6142) {
									c.pcDamage += damage;
									PestControl.portalHealth[0] -= damage;
								}
								if (NPCHandler.npcs[i].npcType == 6143) {
									c.pcDamage += damage;
									PestControl.portalHealth[1] -= damage;
								}
								if (NPCHandler.npcs[i].npcType == 6144) {
									c.pcDamage += damage;
									PestControl.portalHealth[2] -= damage;
								}
								if (NPCHandler.npcs[i].npcType == 6145) {
									c.pcDamage += damage;
									PestControl.portalHealth[3] -= damage;
								}
							}
				boolean dropArrows = true;
						
				for(int noArrowId : c.NO_ARROW_DROP) {
					if(c.lastWeaponUsed == noArrowId) {
						dropArrows = false;
						break;
					}
				}
				if(dropArrows) {
					c.getItems().dropArrowNpc();	
				}
				//if(c.cannonSpec == 1)
				//	Server.playerHandler.players[i].gfx100(89);
				Server.npcHandler.npcs[i].underAttack = true;
				Server.npcHandler.npcs[i].hitDiff = damage;
				Server.npcHandler.npcs[i].HP -= damage;
				
				if (damage2 != -1) {
					if(c.cannonSpec != 1){
						Server.npcHandler.npcs[i].hitDiff = damage2;
						Server.npcHandler.npcs[i].HP -= damage2;
					} else {
						c.cannonSpec = 0;
						final int d2 = damage2;
						final int pId = c.playerId;
						final int index = i;
						int distance = Server.playerHandler.players[pId].distanceToPoint(Server.playerHandler.players[index].getX(), Server.playerHandler.players[index].getY());
						int totalDistance = 2;
						
						CycleEventHandler.getSingleton().addEvent(c, new CycleEvent() {
							@Override
							public void execute(CycleEventContainer c) {
								if(Server.playerHandler.players[index] == null)
									return;
								Server.npcHandler.npcs[index].hitDiff = d2;
								Server.npcHandler.npcs[index].HP -= d2;
								c.stop();
							}

							@Override
							public void stop() {
								
							}
							
						}, totalDistance);
					}
				}
				if (c.killingNpcIndex != c.oldNpcIndex) {
					c.totalDamageDealt = 0;				
				}
				c.killingNpcIndex = c.oldNpcIndex;
				c.totalDamageDealt += damage;
				if(Server.npcHandler.npcs[i].npcType == 2554)
						c.damageToKril += damage;
				Server.npcHandler.npcs[i].hitUpdateRequired = true;
				if (damage2 > -1) {
					Server.npcHandler.npcs[i].hitUpdateRequired2 = true;
					if(Server.npcHandler.npcs[i].npcType == 2554)
						c.damageToKril += damage2;
				}
				Server.npcHandler.npcs[i].updateRequired = true;

			} else if (c.projectileStage > 0) { // magic hit damage
			//double slayerBonus = 1+(c.playerLevel[18] / 99);
				int damage = c.magicDamage;

				if(godSpells()) {
					if(System.currentTimeMillis() - c.godSpellDelay < Config.GOD_SPELL_CHARGE) {
						damage += 5;
					}
				}
				if(Server.npcHandler.npcs[i].npcType == 3847)
			    {
			    	if(damage >= Server.npcHandler.npcs[i].HP && Server.npcHandler.canKillQueen == false) {
			    		damage = Server.npcHandler.npcs[i].HP - 1;
			    	}
			    }
				
				if(c.playerEquipment[c.playerWeapon] == 4170) {
					damage = (int) (damage * 1.5);
				}
				if(c.playerEquipment[c.playerWeapon] == 22494 && (Server.npcHandler.npcs[i].npcType == 2499 || Server.npcHandler.npcs[i].npcType == 2501 || Server.npcHandler.npcs[i].npcType == 2503))
					damage = (int) (damage * .85);
				boolean magicFailed = false;
				if(Server.npcHandler.dagColor != 1 && Server.npcHandler.npcs[i].npcType == 3498){
				damage = 0;
				magicFailed = true;
				c.sendMessage("The dagannoth is currently resistant to that attack!");
				}
				if(Server.npcHandler.dagColor2 != 1 && Server.npcHandler.npcs[i].npcType == 1351){
					damage = 0;
					magicFailed = true;
					c.sendMessage("The dagannoth is currently resistant to that attack!");
				}

				if(Server.npcHandler.npcs[i].npcType == 2554) {
					boolean canAttack = true;
					if(Server.npcHandler.krilWeak == 0 && (c.onLedge() || c.onLedge2() || c.onLedge3()))
							canAttack = false;
						else if(Server.npcHandler.kMinionsDead < 3) {
							damage = 0;
							magicFailed = true;
							c.sendMessage("@red@K'ril Tsutsaroth's defences remain inpenetrable while his minions stand!");
						} else if(Server.npcHandler.krilWeak != 0 && Server.npcHandler.krilWeak != 2)
							canAttack = false;
						else if(Server.npcHandler.krilWeak == 2 && !c.onLedge2())
							canAttack = false;

					if(!canAttack) {
						damage = 0;
						magicFailed = true;
						c.sendMessage("@red@K'ril Tsutsaroth is invulnerable from this point, try attacking from a different height!");
					}
				}
				
				//c.npcIndex = 0;
				int bonusAttack = getBonusAttack(i);
				if (Misc.random(Server.npcHandler.npcs[i].defence) > 10+ Misc.random(mageAtk()) + bonusAttack) {
					damage = 0;
					magicFailed = true;
				} else if (Server.npcHandler.npcs[i].npcType == 2881 || Server.npcHandler.npcs[i].npcType == 2882) {
					damage = 0;
					magicFailed = true;
				}
				
				int totalBarraged = 0;
				for (int j = 0; j < Server.npcHandler.npcs.length; j++) {
					if (Server.npcHandler.npcs[j] != null && Server.npcHandler.npcs[j].MaxHP > 0) {
						int nX = Server.npcHandler.npcs[j].getX();
						int nY = Server.npcHandler.npcs[j].getY();
						int pX = Server.npcHandler.npcs[i].getX();
						int pY = Server.npcHandler.npcs[i].getY();
						
						if ((nX - pX == -1 || nX - pX == 0 || nX - pX == 1) && (nY - pY == -1 || nY - pY == 0 || nY - pY == 1)) {
							if (multis() && Server.npcHandler.npcs[i].inMulti()) {
								Client p = (Client) Server.playerHandler.players[c.playerId];
								appendMultiBarrageNPC(j, c.magicFailed,p);
								totalBarraged++;
								if(Server.npcHandler.goodDistance(Server.npcHandler.npcs[i].absX, Server.npcHandler.npcs[i].absY, c.absX, c.absY, Server.npcHandler.distanceRequired(i)))
									Server.npcHandler.attackPlayer(p, j);
							}
						}
					}
				}
				
				if(totalBarraged > 1)
					return;
				
				if (Server.npcHandler.npcs[i].HP - damage < 0) { 
					damage = Server.npcHandler.npcs[i].HP;
				}
				
			
				
				c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE), 6); 
				c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE/3), 3);
				c.getPA().refreshSkill(3);
				c.getPA().refreshSkill(6);
				if (damage > 0 && PestControl.isInGame(c)) {
								if (NPCHandler.npcs[i].npcType == 6142) {
									c.pcDamage += damage;
									PestControl.portalHealth[0] -= damage;
								}
								if (NPCHandler.npcs[i].npcType == 6143) {
									c.pcDamage += damage;
									PestControl.portalHealth[1] -= damage;
								}
								if (NPCHandler.npcs[i].npcType == 6144) {
									c.pcDamage += damage;
									PestControl.portalHealth[2] -= damage;
								}
								if (NPCHandler.npcs[i].npcType == 6145) {
									c.pcDamage += damage;
									PestControl.portalHealth[3] -= damage;
								}
							}
				if(getEndGfxHeight() == 100 && !magicFailed){ // end GFX
					Server.npcHandler.npcs[i].gfx100(c.MAGIC_SPELLS[c.oldSpellId][5]);
				} else if (!magicFailed){
					Server.npcHandler.npcs[i].gfx0(c.MAGIC_SPELLS[c.oldSpellId][5]);
				}
				
				if(magicFailed) {	
					Server.npcHandler.npcs[i].gfx100(85);
				}			
				if(!magicFailed) {
					int freezeDelay = getFreezeTime();//freeze 
					if(freezeDelay > 0 && Server.npcHandler.npcs[i].freezeTimer == 0) {
						Server.npcHandler.npcs[i].freezeTimer = freezeDelay;
					}
					switch(c.MAGIC_SPELLS[c.oldSpellId][0]) { 
						case 12901:
						case 12919: // blood spells
						case 12911:
						case 12929:
						int heal = Misc.random(damage / 4);
						if(c.playerLevel[3] + heal >= c.getPA().getLevelForXP(c.playerXP[3])) {
							c.playerLevel[3] = c.getPA().getLevelForXP(c.playerXP[3]);
						} else {
							c.playerLevel[3] += heal;
						}
						c.getPA().refreshSkill(3);
						break;
					}

				}
				Server.npcHandler.npcs[i].underAttack = true;
				if(c.MAGIC_SPELLS[c.oldSpellId][6] != 0) {
					Server.npcHandler.npcs[i].hitDiff = damage;
					Server.npcHandler.npcs[i].HP -= damage;
					Server.npcHandler.npcs[i].hitUpdateRequired = true;
					c.totalDamageDealt += damage;
					if(Server.npcHandler.npcs[i].npcType == 2554)
						c.damageToKril += damage;
				}
				c.killingNpcIndex = c.oldNpcIndex;			
				Server.npcHandler.npcs[i].updateRequired = true;
				c.usingMagic = false;
				c.castingMagic = false;
				c.oldSpellId = 0;
			}
		}
	
		if(c.bowSpecShot <= 0) {
			c.oldNpcIndex = 0;
			c.projectileStage = 0;
			c.doubleHit = false;
			c.lastWeaponUsed = 0;
			c.bowSpecShot = 0;
		}
		if(c.bowSpecShot >= 2) {
			c.bowSpecShot = 0;
			//c.attackTimer = getAttackDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
		}
		if(c.bowSpecShot == 1) {
			fireProjectileNpc();
			c.hitDelay = 2;
			c.bowSpecShot = 0;
		}

		if(Server.npcHandler.npcs[i].npcType == 2554) {
			int randomWeak = Server.npcHandler.krilWeak;

			if(Server.npcHandler.npcs[i].HP <= 250 && Server.npcHandler.weakCycle == 3 && Server.npcHandler.weakChanges < 4) {
					do {
						randomWeak = Misc.random(2) + 1;
					} while(randomWeak == Server.npcHandler.krilWeak);

					Server.npcHandler.krilWeak = randomWeak;
					Server.npcHandler.weakCycle = 4;
					Server.npcHandler.npcs[i].defence += 50;
					Server.npcHandler.npcs[i].attack += 100;
					Server.npcHandler.weakChanges++;
					c.sendMessage("@red@You reveal K'ril Tsutsaroth's next vulnerability. It's his "+krilMessage(randomWeak)+"!");
				} else if(Server.npcHandler.npcs[i].HP <= 500 && Server.npcHandler.weakCycle == 2) {
					do {
						randomWeak = Misc.random(2) + 1;
					} while(randomWeak == Server.npcHandler.krilWeak);

					Server.npcHandler.krilWeak = randomWeak;
					Server.npcHandler.weakCycle = 3;
					Server.npcHandler.npcs[i].defence += 50;
					Server.npcHandler.weakChanges++;
					c.sendMessage("@red@You reveal K'ril Tsutsaroth's next vulnerability. It's his "+krilMessage(randomWeak)+"!");
				} else if(Server.npcHandler.npcs[i].HP <= 750 && Server.npcHandler.weakCycle == 1) {
					do {
						randomWeak = Misc.random(2) + 1;
					} while(randomWeak == Server.npcHandler.krilWeak);

					Server.npcHandler.krilWeak = randomWeak;
					Server.npcHandler.weakCycle = 2;
					Server.npcHandler.npcs[i].defence += 50;
					Server.npcHandler.weakChanges++;
					c.sendMessage("@red@You reveal K'ril Tsutsaroth's next vulnerability. It's his "+krilMessage(randomWeak)+"!");
				} else if(Server.npcHandler.npcs[i].HP <= 1000 && Server.npcHandler.weakCycle == 0) {
					do {
						randomWeak = Misc.random(2) + 1;
					} while(randomWeak == Server.npcHandler.krilWeak);

					Server.npcHandler.krilWeak = randomWeak;
					Server.npcHandler.weakCycle = 1;
					Server.npcHandler.npcs[i].defence += 50;
					Server.npcHandler.weakChanges++;
					c.sendMessage("@red@You reveal K'ril Tsutsaroth's next vulnerability. It's his "+krilMessage(randomWeak)+"!");
				} else if(Server.npcHandler.npcs[i].HP <= 1750 && !Server.npcHandler.kMinionsSpawned) {
					int playerAmount = c.countChaosTemple();
					Server.npcHandler.playersInChaos = playerAmount;
					Server.npcHandler.kMinionsSpawned = true;
					Server.npcHandler.krilMinions(playerAmount);
			}
		}

	}

	public String krilMessage(int vul) {
		if(vul == 1)
			return "abdomen";
		else if(vul == 2)
			return "chest";
		else if(vul == 3)
			return "head";

		return "none";
	}
	
		public void applyNpcMeleeDamage2(int i, int damageMask, int damage) {
		c.previousDamage = damage;
		boolean fullVeracsEffect = c.getPA().fullVeracs() && Misc.random(3) == 1;
		if (Server.npcHandler.npcs[i].HP - damage < 0) { 
			damage = Server.npcHandler.npcs[i].HP;
		}
		
		if(c.abbyDagger == 0) {
			if (!fullVeracsEffect) { //abbyDagger will be set to 1 if their next hit is going to avoid defence
				if (Misc.random(Server.npcHandler.npcs[i].defence) > 10 + Misc.random(calculateMeleeAttack())) {
					damage = 0;
				} else if (Server.npcHandler.npcs[i].npcType == 2882 || Server.npcHandler.npcs[i].npcType == 2883) {
					damage = 0;
				} else if(c.playerEquipment[c.playerWeapon] == 13047 || c.playerEquipment[c.playerWeapon] == 19000) {
					c.abbyDagger = 1;
				}
			}
		}


		//c.abbyDagger = 0;

		boolean guthansEffect = false;
		if (c.getPA().fullGuthans()) {
			if (Misc.random(3) == 1) {
				guthansEffect = true;			
			}		
		}
		if(c.fightMode == 3) {
			if(c.worshippedGod == 2) {
				c.getPA().addSkillXP(((damage*Config.MELEE_EXP_RATE*2)/3), 0);
			} else {
				c.getPA().addSkillXP(((damage*Config.MELEE_EXP_RATE)/3), 0);
			}
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 1);
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 2); 				
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(0);
			c.getPA().refreshSkill(1);
			c.getPA().refreshSkill(2);
			c.getPA().refreshSkill(3);
		} else {
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE), c.fightMode); 
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(c.fightMode);
			c.getPA().refreshSkill(3);
		}
		if (damage > 0 && PestControl.isInGame(c)) {
								if (NPCHandler.npcs[i].npcType == 6142) {
									c.pcDamage += damage;
									PestControl.portalHealth[0] -= damage;
								}
								if (NPCHandler.npcs[i].npcType == 6143) {
									c.pcDamage += damage;
									PestControl.portalHealth[1] -= damage;
								}
								if (NPCHandler.npcs[i].npcType == 6144) {
									c.pcDamage += damage;
									PestControl.portalHealth[2] -= damage;
								}
								if (NPCHandler.npcs[i].npcType == 6145) {
									c.pcDamage += damage;
									PestControl.portalHealth[3] -= damage;
								}
							}
		if (damage > 0 && guthansEffect) {
			c.playerLevel[3] += damage;
			if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]))
				c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
			c.getPA().refreshSkill(3);
			Server.npcHandler.npcs[i].gfx0(398);		
		}
		Server.npcHandler.npcs[i].underAttack = true;
		//Server.npcHandler.npcs[i].killerId = c.playerId;
		c.killingNpcIndex = c.npcIndex;
		c.lastNpcAttacked = i;
		switch (c.specEffect) {
			case 4:
				if (damage > 0) {
					if (c.playerLevel[3] + damage > c.getLevelForXP(c.playerXP[3]))
						if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]));
						else 
						c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
					else 
						c.playerLevel[3] += damage;
					c.getPA().refreshSkill(3);
				}
			break;
			case 5:
				c.clawDelay = 2;
			break;
		
		}
		c.specEffect = 0;
		
		switch(damageMask) {
			case 1:
			Server.npcHandler.npcs[i].hitDiff = damage;
			Server.npcHandler.npcs[i].HP -= damage;
			c.totalDamageDealt += damage;
			Server.npcHandler.npcs[i].hitUpdateRequired = true;	
			Server.npcHandler.npcs[i].updateRequired = true;
			break;
		
			case 2:
			Server.npcHandler.npcs[i].hitDiff2 = damage;
			Server.npcHandler.npcs[i].HP -= damage;
			c.totalDamageDealt += damage;
			Server.npcHandler.npcs[i].hitUpdateRequired2 = true;	
			Server.npcHandler.npcs[i].hitUpdateRequired = true;	
			Server.npcHandler.npcs[i].updateRequired = true;
			c.doubleHit = false;
			break;
		}

		if(Server.npcHandler.npcs[i].npcType == 2554)
			c.damageToKril += damage;
	}
	
		public void applyPlayerHit(int i, int damage){
		int damageMask = 1;
		c.previousDamage = damage;
		Client o = (Client) Server.playerHandler.players[i];
		if(o == null) {
			return;
		}
		CycleEventHandler.getSingleton().addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if(c.clawPlayerDelay > 0) {
					c.clawPlayerDelay--;
					if(c.clawPlayerDelay == 0) {
						applyPlayerHit(c.lastAttacked, c.clawDamage[1]);
						applyPlayerHit(c.lastAttacked, c.clawDamage[2]);
					}
				}
			}
			
			@Override
			public void stop() {
				
			}
			
		}, 1);
		int damage1 = damage;
		if (Server.playerHandler.players[i].playerLevel[3] - damage1 < 0) { 
			damage1 = Server.playerHandler.players[i].playerLevel[3];
		}
		if (o.vengOn && damage1 > 0)
			appendVengeance(i, damage1);
		if (damage1 > 0)
			applyRecoil(damage1, i);

		Server.playerHandler.players[i].logoutDelay = System.currentTimeMillis();
		Server.playerHandler.players[i].underAttackBy = c.playerId;
		Server.playerHandler.players[i].killerId = c.playerId;	
		Server.playerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
		if (c.killedBy != Server.playerHandler.players[i].playerId)
			c.totalPlayerDamageDealt = 0;
		c.killedBy = Server.playerHandler.players[i].playerId;
		applySmite(i, damage1);
		switch(damageMask) {
			case 1:
			Server.playerHandler.players[i].dealDamage(damage1);
			Server.playerHandler.players[i].damageTaken[c.playerId] += damage1;
			c.totalPlayerDamageDealt += damage1;
			Server.playerHandler.players[i].updateRequired = true;
			o.getPA().refreshSkill(3);
			break;
		}
		Server.playerHandler.players[i].handleHitMask(damage1);
	}
	
	
	public void applyNpcMeleeDamage(int i, int damageMask) {
		//double slayerBonus = 1+((double)((double)c.playerLevel[18] / 99));
		double slayerBonus = 1.0;
		if(c.getPand().inMission()) slayerBonus = 1.0;
		int damage = (int)((double)((double)Misc.random(calculateMeleeMaxHit())*(slayerBonus)));
		boolean fullVeracsEffect = c.getPA().fullVeracs() && Misc.random(3) == 1;
		if (Server.npcHandler.npcs[i].HP - damage < 0) { 
			damage = Server.npcHandler.npcs[i].HP;
		}
		
		if (!fullVeracsEffect) {
			if (Misc.random(Server.npcHandler.npcs[i].defence) > 10 + Misc.random(calculateMeleeAttack())) {
				damage = 0;
			} else if (Server.npcHandler.npcs[i].npcType == 2882 || Server.npcHandler.npcs[i].npcType == 2883) {
				damage = 0;
			}
		}	
		boolean guthansEffect = false;
		if (c.getPA().fullGuthans()) {
			if (Misc.random(3) == 1) {
				guthansEffect = true;			
			}		
		}
		if(c.fightMode == 3) {
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 0); 
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 1);
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 2); 				
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(0);
			c.getPA().refreshSkill(1);
			c.getPA().refreshSkill(2);
			c.getPA().refreshSkill(3);
		} else {
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE), c.fightMode); 
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(c.fightMode);
			c.getPA().refreshSkill(3);
		}
		if (damage > 0 && PestControl.isInGame(c)) {
								if (NPCHandler.npcs[i].npcType == 6142) {
									c.pcDamage += damage;
									PestControl.portalHealth[0] -= damage;
								}
								if (NPCHandler.npcs[i].npcType == 6143) {
									c.pcDamage += damage;
									PestControl.portalHealth[1] -= damage;
								}
								if (NPCHandler.npcs[i].npcType == 6144) {
									c.pcDamage += damage;
									PestControl.portalHealth[2] -= damage;
								}
								if (NPCHandler.npcs[i].npcType == 6145) {
									c.pcDamage += damage;
									PestControl.portalHealth[3] -= damage;
								}
							}
		if (damage > 0 && guthansEffect) {
			c.playerLevel[3] += damage;
			if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]))
				c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
			c.getPA().refreshSkill(3);
			Server.npcHandler.npcs[i].gfx0(398);		
		}
		Server.npcHandler.npcs[i].underAttack = true;
		//Server.npcHandler.npcs[i].killerId = c.playerId;
		c.killingNpcIndex = c.npcIndex;
		c.lastNpcAttacked = i;
		switch (c.specEffect) {
			case 4:
				if (damage > 0) {
					if (c.playerLevel[3] + damage > c.getLevelForXP(c.playerXP[3]))
						if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]));
						else 
						c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
					else 
						c.playerLevel[3] += damage;
					c.getPA().refreshSkill(3);
				}
			break;
		
		}
		switch(damageMask) {
			case 1:
			Server.npcHandler.npcs[i].hitDiff = damage;
			Server.npcHandler.npcs[i].HP -= damage;
			c.totalDamageDealt += damage;
			Server.npcHandler.npcs[i].hitUpdateRequired = true;	
			//Server.npcHandler.npcs[i].hitUpdateRequired2 = true;	
			Server.npcHandler.npcs[i].updateRequired = true;
			break;
		
			case 2:
			Server.npcHandler.npcs[i].hitDiff2 = damage;
			Server.npcHandler.npcs[i].HP -= damage;
			c.totalDamageDealt += damage;
			Server.npcHandler.npcs[i].hitUpdateRequired2 = true;
			c.doubleHit = false;
			break;
			
		}
	}
	
	public void fireProjectileNpc() {
		if(c.oldNpcIndex > 0) {
			if(Server.npcHandler.npcs[c.oldNpcIndex] != null) {
				c.projectileStage = 2;
				int pX = c.getX();
				int pY = c.getY();
				int nX = Server.npcHandler.npcs[c.oldNpcIndex].getX();
				int nY = Server.npcHandler.npcs[c.oldNpcIndex].getY();
				int offX = (pY - nY)* -1;
				int offY = (pX - nX)* -1;
				c.getPA().createPlayersProjectile(pX, pY, offX, offY, 50, getProjectileSpeed(), getRangeProjectileGFX(), 43, 31, c.oldNpcIndex + 1, getStartDelay());
				if (usingDbow())
					c.getPA().createPlayersProjectile2(pX, pY, offX, offY, 50, getProjectileSpeed(), getRangeProjectileGFX(), 60, 31,  c.oldNpcIndex + 1, getStartDelay(), 35);
			}
		}
	}
	

	
	/**
	* Attack Players, same as npc tbh xD
	**/
	
	public void attackPlayer(int i) {
if(c.inStakeArena()) {
			for(int index = 0; index < c.playerEquipment.length; index++) {
				if(index == 3) {
					if(c.playerEquipment[index] != 1215 && c.playerEquipment[index] != 1231 && c.playerEquipment[index] != 5680 && c.playerEquipment[index] != 5698 && c.playerEquipment[index] != -1)
					{
						c.forcedChat("I'm using a " + c.getItems().getItemName(c.playerEquipment[index]) + "!");
						c.sendMessage("You can't attack someone here while not wearing a dragon dagger!");
						return;
					}
					if(c.playerEquipment[index] == -1) {
						c.forcedChat("I'm not wearing a weapon!");
						return;
					}
				}
				if(index == 12) {
					if(c.playerEquipment[index] != 2550 && c.playerEquipment[index] != -1) {
						c.forcedChat("I'm using a " + c.getItems().getItemName(c.playerEquipment[index]) + "!");
						c.sendMessage("You can't attack someone here while not wearing a Ring of Recoil!");
						return;
					}
					if(c.playerEquipment[index] == -1) {
						c.forcedChat("I'm not using a Ring of Recoil!");
						c.sendMessage("You can't attack someone here while not wearing a Ring of Recoil!");
						return;
					}
				}
				
				if(c.playerEquipment[index] != -1 && index != 12 && index != 3) {
					c.forcedChat("I'm using a " + c.getItems().getItemName(c.playerEquipment[index]) + "!");
					c.sendMessage("You can't attack someone here while wearing a(n) " + c.getItems().getItemName(c.playerEquipment[index]) + "!");
					return;
				}
			}
		}
		
		/*if(c.curseActive[18]) { // SoulSplit GFX's - CAUSES CRASH
if(c.oldPlayerIndex > 0) {
if(Server.playerHandler.players[c.oldPlayerIndex] != null) {
		try {
		final int pX = c.getX();
		final int pY = c.getY();
		final int nX = Server.playerHandler.players[i].getX();
		final int nY = Server.playerHandler.players[i].getY();
		final int offX = (pY - nY)* -1;
		final int offY = (pX - nX)* -1;
		c.SSPLIT = true;
		c.getPA().createPlayersProjectile2(pX, pY, offX, offY, 50, 50, 2263, 9, 9, - c.oldPlayerIndex - 1, 24, 0);
			Server.playerHandler.players[c.oldPlayerIndex].gfx0(2264); // 1738
		c.SSPLIT = false;
} catch (Exception e) {
e.printStackTrace();
}
}
}
}*/

if (Server.playerHandler.players[i] != null) {
Client c2 = (Client)PlayerHandler.players[i]; //the player we are attacking
strBonus = c.playerBonus[10];
calculateMeleeAttack();
calculateRangeAttack();
rangeMaxHit();


if(c.lastWeaponUsed > 862 && c.lastWeaponUsed < 877 || c.lastWeaponUsed == 13022) {
	c.playerIndex = 0;
}



if (Server.playerHandler.players[i].isDead) {
	resetPlayerAttack();
	return;
}

if(c.respawnTimer > 0 || Server.playerHandler.players[i].respawnTimer > 0) {
	resetPlayerAttack();
	return;
}

/*if (c.teleTimer > 0 || Server.playerHandler.players[i].teleTimer > 0) {
	resetPlayerAttack();
	return;
}*/

if(!c.getCombat().checkReqs()) {
	return;
}



boolean sameSpot = c.absX == Server.playerHandler.players[i].getX() && c.absY == Server.playerHandler.players[i].getY();
if(!c.goodDistance(Server.playerHandler.players[i].getX(), Server.playerHandler.players[i].getY(), c.getX(), c.getY(), 25) && !sameSpot) {
	resetPlayerAttack();
	return;
}

if(Server.playerHandler.players[i].respawnTimer > 0) {
	Server.playerHandler.players[i].playerIndex = 0;
	resetPlayerAttack();
	return;
}

if (Server.playerHandler.players[i].heightLevel != c.heightLevel) {
	resetPlayerAttack();
	return;
}
	
//c.sendMessage("Made it here0.");
c.followId = i;
c.followId2 = 0;
if(c.attackTimer <= 0) {
c.lastAttacked = c.playerIndex;
	c.usingBow = false;
	c.specEffect = 0;
	c.usingRangeWeapon = false;
	c.rangeItemUsed = 0;
	boolean usingBow = false;
	boolean usingArrows = false;
	boolean usingOtherRangeWeapons = false;
	boolean usingCross = c.playerEquipment[c.playerWeapon] == 9185 || c.playerEquipment[c.playerWeapon] == 15041;
	c.projectileStage = 0;
	
	if(c.playerEquipment[c.playerWeapon] == 13022){
			usingBow = true;
	}
	
	if (c.absX == Server.playerHandler.players[i].absX && c.absY == Server.playerHandler.players[i].absY) {
		if (c.freezeTimer > 0) {
			resetPlayerAttack();
			return;
		}	
		c.followId = i;
		c.attackTimer = 0;
		return;
	}

	Client o = (Client)Server.playerHandler.players[i];
				if (c.getPA().pathBlocked(c, o)) {
					if(c.freezeTimer <= 0) {
						if ((c.spellId > 0 || c.oldSpellId > 0 || c.usingBow || usingCross || c.usingMagic || usingOtherRangeWeapons || c.autocasting || usingCrystalBow()))
							PathFinder.getPathFinder().findRoute(c, o.absX, o.absY, true, 8, 8);
						if ((c.spellId == 0 && !c.autocasting && !c.usingMagic) && !c.usingBow && !usingOtherRangeWeapons && !usingCrystalBow())
							PathFinder.getPathFinder().findRoute(c, o.absX, o.absY, true, 1, 1);
					} else {
						c.sendMessage("A magical force stops you from moving.");
						resetPlayerAttack();
					}
					c.attackTimer = 0;
					return;
				}
				


	
	/*if ((c.inPirateHouse() && !Server.playerHandler.players[i].inPirateHouse()) || (Server.playerHandler.players[i].inPirateHouse() && !c.inPirateHouse())) {
		resetPlayerAttack();
		return;
	}*/
	//c.sendMessage("Made it here1.");
	
	
		
	if ((c.getX() != c2.getX() && c.getY() != c2.getY() && c.goodDistance(c.getX(), c.getY(), c2.getX(), c2.getY(), 1)) && !usingCross && !usingOtherRangeWeapons && !usingHally() && !usingBow && !c.usingMagic && !usingCrystalBow()) {
			c.faceUpdate(i+32768); //face the player
			if(c.freezeTimer <= 0)
			c.getPA().stopDiagonal(c2.getX(), c2.getY());//move to a correct spot
			return;
	}
	if(!c.usingMagic) {
		for (int bowId : c.BOWS) {
			if(c.playerEquipment[c.playerWeapon] == bowId) {
				usingBow = true;
				c.usingBow = true;
				for (int arrowId : c.ARROWS) {
					if(c.playerEquipment[c.playerArrows] == arrowId) {
						usingArrows = true;
					}
				}
			}
		}				
		
		
		for (int otherRangeId : c.OTHER_RANGE_WEAPONS) {
			if(c.playerEquipment[c.playerWeapon] == otherRangeId) {
				usingOtherRangeWeapons = true;
				c.usingRangeWeapon = true;
			}
		}
	}
	
	if(c.spellId == 0) {
		if (c.autocasting && c.playerMagicBook != 2) {
			c.spellId = c.autocastId;
			c.usingMagic = true;
		}
		if(c.playerEquipment[c.playerWeapon] == 22494) {
			c.autocasting = true;
			c.spellId = 52;
			c.usingMagic = true;
		}
		if(c.playerEquipment[c.playerWeapon] == 19112) {
			c.autocasting = true;
			c.spellId = 53;
			c.usingMagic = true;
		}
		if(c.playerEquipment[c.playerWeapon] == 2415) {
			c.autocasting = true;
			c.spellId = 28;
			c.usingMagic = true;
		}
		if(c.playerEquipment[c.playerWeapon] == 2416) {
			c.autocasting = true;
			c.spellId = 29;
			c.usingMagic = true;
		}
		if(c.playerEquipment[c.playerWeapon] == 2417) {
			c.autocasting = true;
			c.spellId = 30;
			c.usingMagic = true;
		}
	}
	
	//c.sendMessage("Made it here2.");
	if(c.spellId > 0) {
        c.usingMagic = true;
    }
	c.attackTimer = getAttackDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
	c.magicDamage = calculateMagicDamage(c, i);

	if(c.duelRule[9]){
	boolean canUseWeapon = false;
		for(int funWeapon: Config.FUN_WEAPONS) {
			if(c.playerEquipment[c.playerWeapon] == funWeapon) {
				canUseWeapon = true;
			}
		}
		if(!canUseWeapon) {
			c.sendMessage("You can only use fun weapons in this duel!");
			resetPlayerAttack();
			return;
		}
	}
	//c.sendMessage("Made it here3.");
	if(c.duelRule[2] && (usingBow || usingOtherRangeWeapons)) {
		c.sendMessage("Range has been disabled in this duel!");
		return;
	}
	if(c.duelRule[3] && (!usingBow && !usingOtherRangeWeapons && !c.usingMagic)) {
		c.sendMessage("Melee has been disabled in this duel!");
		return;
	}
	
	if(c.duelRule[4] && c.usingMagic) {
		c.sendMessage("Magic has been disabled in this duel!");
		resetPlayerAttack();
		return;
	}
	
	if(c.inBoxIsland() && c.usingMagic) {
		c.sendMessage("You can't use magic here!");
		resetPlayerAttack();
		return;
	}

if(c.inStakeArena() && c.usingMagic) {
		c.sendMessage("You can't use magic here!");
		resetPlayerAttack();
		return;
	}
	
	if(c.inStakeArena() && usingCrystalBow()) {
		c.sendMessage("You can't use the crystal bow here!");
		resetPlayerAttack();
		return;
	}
	
	if((!c.goodDistance(c.getX(), c.getY(), Server.playerHandler.players[i].getX(), Server.playerHandler.players[i].getY(), 4) && (usingOtherRangeWeapons && !usingBow && !c.usingMagic && !usingCrystalBow())) 
	|| (!c.goodDistance(c.getX(), c.getY(), Server.playerHandler.players[i].getX(), Server.playerHandler.players[i].getY(), 2) && (!usingOtherRangeWeapons && usingHally() && !usingBow && !usingCrystalBow() && !c.usingMagic))
	|| (!c.goodDistance(c.getX(), c.getY(), Server.playerHandler.players[i].getX(), Server.playerHandler.players[i].getY(), getRequiredDistance()) && (!usingOtherRangeWeapons && !usingHally() && !usingBow && !c.usingMagic && !usingCrystalBow())) 
	|| (!c.goodDistance(c.getX(), c.getY(), Server.playerHandler.players[i].getX(), Server.playerHandler.players[i].getY(), 10) && (usingBow || c.usingMagic || usingCrystalBow()))) {
		//c.sendMessage("Setting attack timer to 1");
		c.attackTimer = 1;
		if (!usingBow && !c.usingMagic && !usingOtherRangeWeapons && c.freezeTimer > 0)
			resetPlayerAttack();
		return;
	}
	
	if(!usingCross && !usingArrows && usingBow && (c.playerEquipment[c.playerWeapon] < 4212 || c.playerEquipment[c.playerWeapon] > 4223) && c.playerEquipment[c.playerWeapon] != 12926 && c.playerEquipment[c.playerWeapon] != 13022 && !c.usingMagic) {
		c.sendMessage("You have run out of arrows!");
		c.stopMovement();
		resetPlayerAttack();
		return;
	}
	if(correctBowAndArrows() < c.playerEquipment[c.playerArrows] && Config.CORRECT_ARROWS && usingBow && !usingCrystalBow() && c.playerEquipment[c.playerWeapon] != 9185 && c.playerEquipment[c.playerWeapon] != 15041 && !c.usingMagic) {
		c.sendMessage("You can't use "+c.getItems().getItemName(c.playerEquipment[c.playerArrows]).toLowerCase()+"s with a "+c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()+".");
		c.stopMovement();
		resetPlayerAttack();
		return;
	}
	if ((c.playerEquipment[c.playerWeapon] == 9185 || c.playerEquipment[c.playerWeapon] == 15041) && !properBolts() && !c.usingMagic) {
		c.sendMessage("You must use bolts with a crossbow.");
		c.stopMovement();
		resetPlayerAttack();
		return;				
	}
	if (c.playerEquipment[c.playerWeapon] == 13022 && c.playerEquipment[c.playerArrows] > 2){
		c.sendMessage("Your ammunition slot must be empty in order to use hand cannon!");
		c.sendMessage("Hand cannon shots are coming soon!");
		c.stopMovement();
		resetPlayerAttack();
		return;	
	}
	
	if(usingBow || c.spellId > 0 || usingOtherRangeWeapons || usingHally()) {
		c.stopMovement();
	}
	
	if(!checkMagicReqs(c.spellId)) {
		c.stopMovement();
		resetPlayerAttack();
		return;
	}
	
	c.faceUpdate(i+32768);
	
	if(c.duelStatus != 5) {
		if(!c.attackedPlayers.contains(c.playerIndex) && !Server.playerHandler.players[c.playerIndex].attackedPlayers.contains(c.playerId)) {
			c.attackedPlayers.add(c.playerIndex);
			c.isSkulled = true;
			c.skullTimer = Config.SKULL_TIMER;
			if(c.redSkull != 1)
			c.headIconPk = 0;
			c.getPA().requestUpdates();
		} 
	}
	c.specAccuracy = 1.0;
	c.specDamage = 1.0;
	c.delayedDamage = c.delayedDamage2 = 0;
	if(c.usingSpecial && !c.usingMagic && c.playerEquipment[c.playerWeapon] != 15042) {
		/*if(c.underAttackBy == 0 && c.isInEdge()) {//anti-rush, testing how this works out
			c.sendMessage("No rushing in Edgeville!");//well it didn't work out...
			c.usingSpecial = false;
			c.getItems().updateSpecialBar();
			resetPlayerAttack();
			return;
		}*/
		if(c.duelRule[10] && c.duelStatus == 5) {
			c.sendMessage("Special attacks have been disabled during this duel!");
			c.usingSpecial = false;
			c.getItems().updateSpecialBar();
			resetPlayerAttack();
			return;
		}
		if(checkSpecAmount(c.playerEquipment[c.playerWeapon])){
			c.lastArrowUsed = c.playerEquipment[c.playerArrows];
			activateSpecial(c.playerEquipment[c.playerWeapon], i);
			c.followId = c.playerIndex;
			return;
		} else {
			c.sendMessage("You don't have the required special energy to use this attack.");
			c.usingSpecial = false;
			c.getItems().updateSpecialBar();
			c.playerIndex = 0;
			return;
		}	
	}
	
	if(!c.usingMagic && c.playerEquipment[c.playerWeapon] != 22494 && c.playerEquipment[c.playerWeapon] != 2415 && c.playerEquipment[c.playerWeapon] != 2416 && c.playerEquipment[c.playerWeapon] != 2417) {
		c.startAnimation(getWepAnim(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()));
		c.mageFollow = false;
	} else {
		c.startAnimation(c.MAGIC_SPELLS[c.spellId][2]);
		c.mageFollow = true;
		c.followId = c.playerIndex;
	}
	
	Server.playerHandler.players[i].underAttackBy = c.playerId;
	Server.playerHandler.players[i].logoutDelay = System.currentTimeMillis();
	Server.playerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
	Server.playerHandler.players[i].killerId = c.playerId;
	c.lastArrowUsed = 0;
	c.rangeItemUsed = 0;
	if(!usingBow && !c.usingMagic && !usingOtherRangeWeapons) { // melee hit delay
		c.followId = Server.playerHandler.players[c.playerIndex].playerId;
		c.getPA().followPlayer();
		c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
		c.delayedDamage = Misc.random(calculateMeleeMaxHit());
		c.projectileStage = 0;
		c.oldPlayerIndex = i;
		if(c.delayedDamage > 0 && c.playerEquipment[c.playerWeapon] == 15042 && Misc.random(10) <= 1) {
			c.playerLevel[3] += (int)(Math.round(c.delayedDamage * .15) + 1);
			if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]))
				c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
			c.updateRequired = true;
			c.getPA().refreshSkill(3);
			Server.playerHandler.players[i].gfx0(754);
		}
	}
					
	if((usingBow || usingCross) && (!usingOtherRangeWeapons && !c.usingMagic)) { // range hit delay
		if(c.playerEquipment[c.playerWeapon] >= 4212 && c.playerEquipment[c.playerWeapon] <= 4223) {
			c.rangeItemUsed = c.playerEquipment[c.playerWeapon];
			c.crystalBowArrowCount++;
		} else if(c.playerEquipment[c.playerWeapon] == 12926) {
			c.rangeItemUsed = c.playerEquipment[c.playerWeapon];
		} else {
			c.rangeItemUsed = c.playerEquipment[c.playerArrows];
			c.getItems().deleteArrow();
		}
		if (c.fightMode == 2)
			c.attackTimer--;
		if (usingCross)
			c.usingBow = true;
		c.followId = Server.playerHandler.players[c.playerIndex].playerId;
		c.getPA().followPlayer();
		c.lastWeaponUsed = c.playerEquipment[c.playerWeapon];
		c.lastArrowUsed = c.playerEquipment[c.playerArrows];
		c.gfx100(getRangeStartGFX());	
		c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
		c.projectileStage = 1;
		c.oldPlayerIndex = i;
		fireProjectilePlayer();
	}
	
	if(c.playerEquipment[c.playerWeapon] == 13022 && (!usingOtherRangeWeapons && !c.usingMagic)){
		c.rangeItemUsed = c.playerEquipment[c.playerWeapon];
		c.usingRangeWeapon = true;
		c.usingBow = true;
		c.followId = Server.playerHandler.players[c.playerIndex].playerId;
		c.getPA().followPlayer();
		c.gfx100(getRangeStartGFX());
		if (c.fightMode == 2)
			c.attackTimer--;
		c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
		c.projectileStage = 1;
		c.oldPlayerIndex = i;
		fireProjectilePlayer();
	}
								
	if(usingOtherRangeWeapons && (!usingCross && !usingBow && !c.usingMagic)) {	// knives, darts, etc hit delay
		c.rangeItemUsed = c.playerEquipment[c.playerWeapon];
		c.getItems().deleteEquipment();
		c.usingRangeWeapon = true;
		c.followId = Server.playerHandler.players[c.playerIndex].playerId;
		c.getPA().followPlayer();
		c.gfx100(getRangeStartGFX());
		if (c.fightMode == 2)
			c.attackTimer--;
		c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
		c.projectileStage = 1;
		c.oldPlayerIndex = i;
		fireProjectilePlayer();
	}
	
	if (usingBow || usingCross || c.usingMagic || usingOtherRangeWeapons) {
		c.getPA().followPlayer();
		c.stopMovement();
	} else {
		c.followId = i;
		c.followId2 = 0;
	}

	//if()


	if(c.usingMagic) {	// magic hit delay
		int pX = c.getX();
		int pY = c.getY();
		int nX = Server.playerHandler.players[i].getX();
		int nY = Server.playerHandler.players[i].getY();
		int offX = (pY - nY)* -1;
		int offY = (pX - nX)* -1;
		c.castingMagic = true;
		c.projectileStage = 2;
		if(c.MAGIC_SPELLS[c.spellId][3] > 0) {
			if(getStartGfxHeight() == 100) {
				c.gfx100(c.MAGIC_SPELLS[c.spellId][3]);
			} else {
				c.gfx0(c.MAGIC_SPELLS[c.spellId][3]);
			}
		}
		if(c.MAGIC_SPELLS[c.spellId][4] > 0) {
			c.getPA().createPlayersProjectile(pX, pY, offX, offY, 50, 78, c.MAGIC_SPELLS[c.spellId][4], getStartHeight(), getEndHeight(), -i - 1, getStartDelay());
		}
		if (c.autocastId > 0 || c.castingMagic) {
			c.followId = c.playerIndex;
			c.followDistance = 5;
		}	
		c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
		c.oldPlayerIndex = i;
		c.oldSpellId = c.spellId;
        c.spellId = 0;
		//Client o = (Client)Server.playerHandler.players[i];
		
		if(c.MAGIC_SPELLS[c.oldSpellId][0] == 12891 && o.isMoving) {
			//c.sendMessage("Barrage projectile..");
			c.getPA().createPlayersProjectile(pX, pY, offX, offY, 50, 85, 368, 25, 25, -i - 1, getStartDelay());
		}
		/*if(Misc.random(o.getCombat().mageDef()) > Misc.random(mageAtk())) {
			c.magicFailed = true;
		} else {
			c.magicFailed = false;
		}*/
		int freezeDelay = getFreezeTime();//freeze time
		if(freezeDelay > 0 && Server.playerHandler.players[i].freezeTimer <= -3 && !c.magicFailed) { 
			Server.playerHandler.players[i].freezeTimer = freezeDelay;
			//o.resetWalkingQueue();
			o.stopMovement();
			o.sendMessage("You have been frozen.");
			o.frozenBy = c.playerId;
		}
		if (!c.autocasting && c.spellId <= 0) {
			c.playerIndex = 0;
			c.stopMovement();
		}
	}
	if(usingBow && Config.CRYSTAL_BOW_DEGRADES) { // crystal bow degrading
		if(c.playerEquipment[c.playerWeapon] == 4212) { // new crystal bow becomes full bow on the first shot
			c.getItems().wearItem(4214, 1, 3);
		}
		
		if(c.crystalBowArrowCount >= 250){
			switch(c.playerEquipment[c.playerWeapon]) {
				
				case 4223: // 1/10 bow
				c.getItems().wearItem(-1, 1, 3);
				c.sendMessage("Your crystal bow has fully degraded.");
				if(!c.getItems().addItem(4207, 1)) {
					Server.itemHandler.createGroundItem(c, 4207, c.getX(), c.getY(), 1, c.getId());
				}
				c.crystalBowArrowCount = 0;
				break;
				
				default:
				c.getItems().wearItem(++c.playerEquipment[c.playerWeapon], 1, 3);
				c.sendMessage("Your crystal bow degrades.");
				c.crystalBowArrowCount = 0;
				break;
			}
		}	
	}
}
}
}
	
	public boolean usingCrystalBow() {
		return c.playerEquipment[c.playerWeapon] >= 4212 && c.playerEquipment[c.playerWeapon] <= 4223;	
	}
	
	public void appendVengeance(int otherPlayer, int damage) {
		if (damage <= 0)
			return;
		Player o = Server.playerHandler.players[otherPlayer];
		o.forcedText = "Taste Vengeance!";
		o.forcedChatUpdateRequired = true;
		o.updateRequired = true;
		o.vengOn = false;
		
		/*if(c.playerEquipment[o.playerWeapon] == 22494){
			if(damage > 30)
				damage = 30 - Misc.random(5);
		}*/
		
		if ((o.playerLevel[3] - damage) <= 0)
			damage = o.playerLevel[3];
			damage = (int)(damage * 0.75);
			if (damage > c.playerLevel[3]) {
				damage = c.playerLevel[3];
			}
			c.setHitDiff2(damage);
			c.setHitUpdateRequired2(true);
			c.playerLevel[3] -= damage;
			c.getPA().refreshSkill(3);
			c.updateRequired = true;
	}

	public int calculateMagicDamage(Client c, int i) {
		Client o = (Client) Server.playerHandler.players[i];
		int damage = 0;
		c.oldSpellId = c.spellId;

		if(o != null) {
			if(Misc.random(o.getCombat().mageDef()) > Misc.random(mageAtk())) {
				c.magicFailed = true;
			} else {
				c.magicFailed = false;
			}
		} else {
			if(Server.npcHandler.npcs[i] != null) {
				if(Misc.random(Server.npcHandler.npcs[i].defence) > Misc.random(mageAtk())) {
					c.magicFailed = true;
				} else {
					c.magicFailed = false;
				}
			}
		}

		if(c.magicFailed) {
			damage = 0;
		} else {
			damage = Misc.random(finalMagicDamage(c));
		}

		if(damage == 0)
			c.magicFailed = true;

		c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE), 6); 
		c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE/3), 3);
		c.getPA().refreshSkill(3);
		c.getPA().refreshSkill(6);

		return damage;
	}
	
	public void playerDelayedHit(int i) {
		if (Server.playerHandler.players[i] != null) {
			if (Server.playerHandler.players[i].isDead || c.isDead || Server.playerHandler.players[i].playerLevel[3] <= 0 || c.playerLevel[3] <= 0) {
				c.playerIndex = 0;
				return;
			}
			if (Server.playerHandler.players[i].respawnTimer > 0) {
				c.faceUpdate(0);
				c.playerIndex = 0;
				return;
			}
			Client o = (Client) Server.playerHandler.players[i];
			o.getPA().removeAllWindows();
			if (o.playerIndex <= 0 && o.npcIndex <= 0) {
				if (o.autoRet == 1 && !o.inMageArena() && !o.autocasting) {
					o.playerIndex = c.playerId;
				}
				/*if(o.autoRet == 1 && o.inMageArena() && o.autocasting)
				{
					o.sendMessage("You can't use m");
				}*/
			}
			
			if(o.attackTimer <= 3 || o.attackTimer == 0 && o.playerIndex == 0 && !c.castingMagic) { // block animation
				o.startAnimation(o.getCombat().getBlockEmote());
			}
			if(o.inTrade) {
				o.getTradeAndDuel().declineTrade();
			}
			if(c.projectileStage == 0 && !c.usingMagic && !c.castingMagic) { // melee hit damage
					int damageOnPlayer = (int)Math.round(Misc.random(calculateMeleeMaxHit())*.85);
					
					 if(damageOnPlayer >= 40)
     						damageOnPlayer = 40;

					c.clawDamage[0] = (int)Math.round(damageOnPlayer / 2);
					c.clawDamage[1] = (int)Math.round(damageOnPlayer / 4);
					c.clawDamage[2] = (int)Math.round(damageOnPlayer / 4);
					applyPlayerMeleeDamage2(i, 1, damageOnPlayer);
					if(c.doubleHit && !c.usingClaws) {
						if(c.playerEquipment[c.playerWeapon] == 13047 || c.playerEquipment[c.playerWeapon] == 19000) {
							if(c.abbyDagger == 1) {
								applyPlayerMeleeDamage2(i, 2, Misc.random(calculateMeleeMaxHit())+1);
								c.abbyDagger = 0;
							} else {
								applyPlayerMeleeDamage2(i, 2, 0);
							}
						} else {
							applyPlayerMeleeDamage2(i, 2, Misc.random(calculateMeleeMaxHit()));
						}
					}
					if(c.doubleHit && c.usingClaws) {
						applyPlayerHit(i, c.clawDamage[0]);
						c.clawPlayerDelay = 1;
						c.usingClaws = false;
					}
			}
			
			
			
			if(!c.castingMagic && c.projectileStage > 0) { // range hit damage
				int damage = Misc.random(rangeMaxHit());
				int damage2 = -1;
				
				if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1 || c.cannonSpec == 1) {
					damage2 = Misc.random(rangeMaxHit());
				}
					
					
					if (c.lastWeaponUsed == 11235 && c.playerEquipment[c.playerWeapon] != 11235){
					damage = (int)(damage*.5);
					damage2 = (int)(damage2*.5);
					}
				boolean ignoreDef = false;
				if (Misc.random(4) == 1 && c.lastArrowUsed == 9243 && (c.playerEquipment[c.playerWeapon] == 9185 || c.playerEquipment[c.playerWeapon] == 15041)) {
					ignoreDef = true;
					o.gfx0(758);
				}					
				if(Misc.random(10+o.getCombat().calculateRangeDefence()) > Misc.random(10+calculateRangeAttack()) && !ignoreDef) {
					damage = 0;
				}
				if (Misc.random(4) == 1 && c.lastArrowUsed == 9242 && damage > 0 && !usingCrystalBow()) {
					Server.playerHandler.players[i].gfx0(754);
					damage = Server.playerHandler.players[i].playerLevel[3]/3;
					c.handleHitMask(c.playerLevel[3]/10);
					c.dealDamage(c.playerLevel[3]/10);
					c.gfx0(754);
				}
				
				if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1) {
					if (Misc.random(10+o.getCombat().calculateRangeDefence()) > Misc.random(10+calculateRangeAttack()))
						damage2 = 0;
				}
				
				if (c.dbowSpec) {
					o.gfx100(1100);
					if (damage < 8)
						damage = 8;
					if (damage2 < 8)
						damage2 = 8;
					c.dbowSpec = false;
				}
				if (c.lastArrowUsed == 9244 && ((c.lastWeaponUsed == 9185 && Misc.random(4) == 1) || (c.lastWeaponUsed == 15041 && Misc.random(3) == 1))) {
				if(c.lastWeaponUsed == 4214) {
				return;
}					if(o.getPA().antiFire() == 0){
					damage *= 1.5;
					o.sendMessage("@red@You are badly burnt by the dragon fire.");
					}
					if(o.getPA().antiFire() == 1){
					damage *= 1.45;
					o.sendMessage("@red@You are slightly protected from the dragon fire.");
					}
					if(o.getPA().antiFire() == 2){
					damage *= 1.4;
					o.sendMessage("@red@The dragon fire is weak against you.");
					}
					o.gfx0(756);
				}
				if(o.prayerActive[17] && System.currentTimeMillis() - o.protRangeDelay > 1500) { // if prayer active reduce damage by half 
					damage = (int)damage * 40 / 100;
					if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1 || c.cannonSpec == 1)
						damage2 = (int)damage2 * 40 / 100;
				}
				if (Server.playerHandler.players[i].playerLevel[3] - damage < 0) { 
					damage = Server.playerHandler.players[i].playerLevel[3];
				}
				if (Server.playerHandler.players[i].playerLevel[3] - damage - damage2 < 0) { 
					damage2 = Server.playerHandler.players[i].playerLevel[3] - damage;
				}
				if (damage > 0 && c.lastArrowUsed == 9245 && ((c.lastWeaponUsed == 9185 && Misc.random(6) == 1) || (c.lastWeaponUsed == 15041 && Misc.random(5) == 1))) {
					if(c.lastWeaponUsed == 4214)
						return;
					int greatDamage = (int) (damage *= 1.25);
					int hpHeal = (int) (greatDamage * 0.25);
					 if (c.playerLevel[3] <= 99) {
						if (c.playerLevel[3] + hpHeal >= c.getPA().getLevelForXP(
								c.playerXP[3])) {
							c.playerLevel[3] = c.getPA().getLevelForXP(
									c.playerXP[3]);
									c.getPA().refreshSkill(3);
									c.sendMessage("Your Onyx bolts (e) heal you for a portion of the damage dealt.");
						}
					}
					o.gfx0(753);
				}
				if (damage < 0)
					damage = 0;
				if (damage2 < 0 && damage2 != -1)
					damage2 = 0;
				if (o.vengOn) {
					appendVengeance(i, damage);
					if(c.cannonSpec != 1)
						appendVengeance(i, damage2);
				}
				if (damage > 0)
					applyRecoil(damage, i);
				if (damage2 > 0)
					applyRecoil(damage2, i);
				if(c.fightMode == 3) {
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 4); 
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 1);				
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 3);
					c.getPA().refreshSkill(1);
					c.getPA().refreshSkill(3);
					c.getPA().refreshSkill(4);
				} else {
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE), 4); 
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 3);
					c.getPA().refreshSkill(3);
					c.getPA().refreshSkill(4);
				}
				boolean dropArrows = true;
						
				for(int noArrowId : c.NO_ARROW_DROP) {
					if(c.lastWeaponUsed == noArrowId) {
						dropArrows = false;
						break;
					}
				}
				if(dropArrows) {
					c.getItems().dropArrowPlayer();	
				}
				if(c.cannonSpec == 1)
					Server.playerHandler.players[i].gfx100(89);
				Server.playerHandler.players[i].underAttackBy = c.playerId;
				Server.playerHandler.players[i].logoutDelay = System.currentTimeMillis();
				Server.playerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
				Server.playerHandler.players[i].killerId = c.playerId;
				//Server.playerHandler.players[i].setHitDiff(damage);
				//Server.playerHandler.players[i].playerLevel[3] -= damage;
				Server.playerHandler.players[i].dealDamage(damage);
				Server.playerHandler.players[i].damageTaken[c.playerId] += damage;
				c.killedBy = Server.playerHandler.players[i].playerId;
				Server.playerHandler.players[i].handleHitMask(damage);
				if (damage2 != -1) {
					//Server.playerHandler.players[i].playerLevel[3] -= damage2;
					if(c.cannonSpec != 1){
						Server.playerHandler.players[i].dealDamage(damage2);
						Server.playerHandler.players[i].damageTaken[c.playerId] += damage2;
						Server.playerHandler.players[i].handleHitMask(damage2);
					} else {
						c.cannonSpec = 0;
						final int d2 = damage2;
						final int pId = c.playerId;
						final int index = i;
						int distance = Server.playerHandler.players[pId].distanceToPoint(Server.playerHandler.players[index].getX(), Server.playerHandler.players[index].getY());
						int totalDistance = 2;
						
						CycleEventHandler.getSingleton().addEvent(c, new CycleEvent() {
							@Override
							public void execute(CycleEventContainer c) {
								if(Server.playerHandler.players[index] == null)
									return;
								Server.playerHandler.players[index].gfx100(89);
								Server.playerHandler.players[index].dealDamage(d2);
								Server.playerHandler.players[index].damageTaken[pId] += d2;
								Server.playerHandler.players[index].handleHitMask(d2);
								Server.playerHandler.players[index].updateRequired = true;
								c.stop();
							}

							@Override
							public void stop() {
								// TODO Auto-generated method stub
								
							}
							
						}, totalDistance);
					}
				}
				o.getPA().refreshSkill(3);
					
				//Server.playerHandler.players[i].setHitUpdateRequired(true);	
				Server.playerHandler.players[i].updateRequired = true;
				applySmite(i, damage);
				if (damage2 != -1)
					applySmite(i, damage2);
			
			} else if (c.projectileStage > 0) { // magic hit damage
				int damage = c.magicDamage;

				if(godSpells()) {
					if(System.currentTimeMillis() - c.godSpellDelay < Config.GOD_SPELL_CHARGE) {
						damage += 5;
					}
				}

				//c.playerIndex = 0;

				if (c.playerMagicBook == 2 && c.playerEquipment[c.playerWeapon] != 22494){
					damage = 0;
					c.sendMessage("The only combat spell you can cast on Lunar is Polypore strike.");
				}
				
				if (c.playerMagicBook != 0 && (c.playerEquipment[c.playerWeapon] == 2415 || c.playerEquipment[c.playerWeapon] == 2416 || c.playerEquipment[c.playerWeapon] == 2417)){
					damage = 0;
					c.sendMessage("You must be on the modern spellbook to cast this spell.");
				}
				
				if (c.magicFailed)
					damage = 0;
					
				if(o.prayerActive[16] && System.currentTimeMillis() - o.protMageDelay > 1500) { // if prayer active reduce damage by half 
					damage = (int)damage * 40 / 100;
				}

				if (Server.playerHandler.players[i].playerLevel[3] - damage < 0) {
					damage = Server.playerHandler.players[i].playerLevel[3];
				}
				if (o.vengOn)
					appendVengeance(i, damage);
				if (damage > 0)
					applyRecoil(damage, i);
				
				if(getEndGfxHeight() == 100 && !c.magicFailed){ // end GFX
					Server.playerHandler.players[i].gfx100(c.MAGIC_SPELLS[c.oldSpellId][5]);
				} else if (!c.magicFailed){
					Server.playerHandler.players[i].gfx0(c.MAGIC_SPELLS[c.oldSpellId][5]);
				} else if(c.magicFailed) {	
					Server.playerHandler.players[i].gfx100(85);
				}
				
				if(!c.magicFailed) {
					if(System.currentTimeMillis() - Server.playerHandler.players[i].reduceStat > 35000) {
						Server.playerHandler.players[i].reduceStat = System.currentTimeMillis();
						switch(c.MAGIC_SPELLS[c.oldSpellId][0]) { 
							case 12987:
							case 13011:
							case 12999:
							case 13023:
							Server.playerHandler.players[i].playerLevel[0] -= ((o.getPA().getLevelForXP(Server.playerHandler.players[i].playerXP[0]) * 10) / 100);
							break;
						}
					}
					
					switch(c.MAGIC_SPELLS[c.oldSpellId][0]) { 	
						case 12445: //teleblock
						if (c.playerMagicBook == 0){
						if (System.currentTimeMillis() - o.teleBlockDelay > o.teleBlockLength) {
							o.teleBlockDelay = System.currentTimeMillis();
							o.sendMessage("You have been teleblocked.");
							if (o.prayerActive[16] && System.currentTimeMillis() - o.protMageDelay > 1500)
								o.teleBlockLength = 150000;
							else
								o.teleBlockLength = 300000;
						}
						}
						break;

						
						case 12901:
						case 12919: // blood spells
						case 12911:
						case 12929:
						int heal = (int)(damage / 4);
						if(c.playerLevel[3] + heal > c.getPA().getLevelForXP(c.playerXP[3])) {
							c.playerLevel[3] = c.getPA().getLevelForXP(c.playerXP[3]);
						} else {
							c.playerLevel[3] += heal;
						}
						c.getPA().refreshSkill(3);
						break;
						
						case 1153:						
						Server.playerHandler.players[i].playerLevel[0] -= ((o.getPA().getLevelForXP(Server.playerHandler.players[i].playerXP[0]) * 5) / 100);
						o.sendMessage("Your attack level has been reduced!");
						Server.playerHandler.players[i].reduceSpellDelay[c.reduceSpellId] = System.currentTimeMillis();
						o.getPA().refreshSkill(0);
						break;
						
						case 1157:
						Server.playerHandler.players[i].playerLevel[2] -= ((o.getPA().getLevelForXP(Server.playerHandler.players[i].playerXP[2]) * 5) / 100);
						o.sendMessage("Your strength level has been reduced!");
						Server.playerHandler.players[i].reduceSpellDelay[c.reduceSpellId] = System.currentTimeMillis();						
						o.getPA().refreshSkill(2);
						break;
						
						case 1161:
						Server.playerHandler.players[i].playerLevel[1] -= ((o.getPA().getLevelForXP(Server.playerHandler.players[i].playerXP[1]) * 5) / 100);
						o.sendMessage("Your defence level has been reduced!");
						Server.playerHandler.players[i].reduceSpellDelay[c.reduceSpellId] = System.currentTimeMillis();					
						o.getPA().refreshSkill(1);
						break;
						
						case 1542:
						Server.playerHandler.players[i].playerLevel[1] -= ((o.getPA().getLevelForXP(Server.playerHandler.players[i].playerXP[1]) * 10) / 100);
						o.sendMessage("Your defence level has been reduced!");
						Server.playerHandler.players[i].reduceSpellDelay[c.reduceSpellId] =  System.currentTimeMillis();
						o.getPA().refreshSkill(1);
						break;
						
						case 1543:
						Server.playerHandler.players[i].playerLevel[2] -= ((o.getPA().getLevelForXP(Server.playerHandler.players[i].playerXP[2]) * 10) / 100);
						o.sendMessage("Your strength level has been reduced!");
						Server.playerHandler.players[i].reduceSpellDelay[c.reduceSpellId] = System.currentTimeMillis();
						o.getPA().refreshSkill(2);
						break;
						
						case 1562:					
						Server.playerHandler.players[i].playerLevel[0] -= ((o.getPA().getLevelForXP(Server.playerHandler.players[i].playerXP[0]) * 10) / 100);
						o.sendMessage("Your attack level has been reduced!");
						Server.playerHandler.players[i].reduceSpellDelay[c.reduceSpellId] = System.currentTimeMillis();					
						o.getPA().refreshSkill(0);
						break;
					}					
				}
				
				int damageCap = 55;//poly cap
				int damageCap2 = 65;

				if(o.playerEquipment[o.playerShield] == 6259 || o.playerEquipment[o.playerShield] == 6261 || 
			      o.playerEquipment[o.playerShield] == 6263 || o.playerEquipment[o.playerShield] == 6265 || 
			      o.playerEquipment[o.playerShield] == 6267 || o.playerEquipment[o.playerShield] == 6269 || 
			      o.playerEquipment[o.playerShield] == 6271 || o.playerEquipment[o.playerShield] == 6273 || 
			      o.playerEquipment[o.playerShield] == 6275 || o.playerEquipment[o.playerShield] == 6277 || 
			      o.playerEquipment[o.playerShield] == 6279)
			     {
			       damageCap = (int)(damageCap * .5);
			       damageCap2 = (int)(damageCap2 * .5);
			     }

				if((damage > damageCap) && (c.playerEquipment[c.playerWeapon] == 22494 || c.playerEquipment[c.playerWeapon] == 19112))
					damage = damageCap;
				else if((damage > damageCap2) && (c.playerEquipment[c.playerWeapon] != 22494 && c.playerEquipment[c.playerWeapon] != 19112))
					damage = damageCap2;
				
				
				Server.playerHandler.players[i].logoutDelay = System.currentTimeMillis();
				Server.playerHandler.players[i].underAttackBy = c.playerId;
				Server.playerHandler.players[i].killerId = c.playerId;
				Server.playerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
				if(c.MAGIC_SPELLS[c.oldSpellId][6] != 0) {
					//Server.playerHandler.players[i].playerLevel[3] -= damage;
					Server.playerHandler.players[i].dealDamage(damage);
					Server.playerHandler.players[i].damageTaken[c.playerId] += damage;
					c.totalPlayerDamageDealt += damage;
					if (!c.magicFailed) {
						//Server.playerHandler.players[i].setHitDiff(damage);
						//Server.playerHandler.players[i].setHitUpdateRequired(true);
						Server.playerHandler.players[i].handleHitMask(damage);
					}
				}
				applySmite(i, damage);
				c.killedBy = Server.playerHandler.players[i].playerId;	
				o.getPA().refreshSkill(3);
				Server.playerHandler.players[i].updateRequired = true;
				c.usingMagic = false;
				c.castingMagic = false;
				if (o.inMulti() && multis()) {
					c.barrageCount = 0;
					for (int j = 0; j < Server.playerHandler.players.length; j++) {
						if (Server.playerHandler.players[j] != null) {
							if (j == o.playerId)
								continue;
							if (c.barrageCount >= 9)
								break;
							if (o.goodDistance(o.getX(), o.getY(), Server.playerHandler.players[j].getX(), Server.playerHandler.players[j].getY(), 1))
								appendMultiBarrage(j, c.magicFailed);
						}	
					}
				}
				c.getPA().refreshSkill(3);
				c.getPA().refreshSkill(6);
				c.oldSpellId = 0;
			}
		}	
		c.getPA().requestUpdates();
		int oldindex = c.oldPlayerIndex;
		if(c.bowSpecShot <= 0) {
			c.oldPlayerIndex = 0;	
			c.projectileStage = 0;
			c.lastWeaponUsed = 0;
			c.doubleHit = false;
			c.bowSpecShot = 0;
		}
		if(c.bowSpecShot != 0) {
			c.bowSpecShot = 0;
		}
	}
	
	public static int finalMagicDamage(Client c) {
		double damage = c.MAGIC_SPELLS[c.oldSpellId][6];
		double damageMultiplier = 1;
		
		if (c.playerLevel[c.playerMagic] > c.getLevelForXP(c.playerXP[6]) && c.getLevelForXP(c.playerXP[6]) >= 50)
			damageMultiplier += .03 * (c.playerLevel[c.playerMagic] - c.getLevelForXP(c.playerXP[c.playerMagic]));
		else
			damageMultiplier = 1;

		if (c.prayerActive[20])
			damageMultiplier += .05;
		
		switch (c.playerEquipment[c.playerWeapon]) {
			case 4675: // Ancient Staff
			case 6914: // Master Wand
			case 8841: // Void Knight Mace
				damageMultiplier += .05;
			break;
			case 4710: // Ahrim's Staff
			case 4862: // Ahrim's Staff
			case 4864: // Ahrim's Staff
			case 4865: // Ahrim's Staff
				damageMultiplier += .075;
			break;
			case 15001: // Staff of Light
				damageMultiplier += .10;
			break;
			case 15040: // Chaotic Staff
				damageMultiplier += .15;
			break;
		}
		
		switch (c.playerEquipment[c.playerAmulet]) {
			case 19114:
			case 15000: // Arcane Stream
				damageMultiplier += .15;
			break;
		}
		
		switch (c.playerEquipment[c.playerHands]) {
			case 777:
				damageMultiplier += .075;
			break;
		}

		switch (c.playerEquipment[c.playerShield]) {
			case 6889:
				damageMultiplier += .05;
			break;
		}
		
		if(c.playerEquipment[c.playerWeapon] != 22494 && c.oldSpellId == 52)
			damageMultiplier = 0.0;

		damage *= damageMultiplier;

		return (int) damage;
	}
	
		public void applyPlayerMeleeDamage2(int i, int damageMask, int damage){
		c.previousDamage = damage;
		Client o = (Client) Server.playerHandler.players[i];
		if(o == null) {
			return;
		}
		int damage1 = 0;
		boolean veracsEffect = false;
		boolean guthansEffect = false;

		if (c.getPA().fullVeracs()) {
			if (Misc.random(4) == 1) {
				veracsEffect = true;				
			}		
		}
		if (c.getPA().fullGuthans()) {
			if (Misc.random(4) == 1) {
				guthansEffect = true;
			}		
		}
		if (damageMask == 1) {
			damage1 = c.delayedDamage;
			c.delayedDamage = 0;
		} else {
			damage1 = c.delayedDamage2;
			c.delayedDamage2 = 0;
		}
		if((Misc.random(o.getCombat().calculateMeleeDefence()) > Misc.random(calculateMeleeAttack()) && !veracsEffect && c.abbyDagger == 0) || c.abbyDagger == 2) {
			if(c.abbyDagger == 0 && (c.playerEquipment[c.playerWeapon] == 13047 || c.playerEquipment[c.playerWeapon] == 19000)) {
				damage1 = 0;
				c.bonusAttack = 0;
				c.abbyDagger = 2;
			} else if(c.abbyDagger == 2) {
				damage1 = 0;
				c.bonusAttack = 0;
				c.abbyDagger = 0;
			} else {
				damage1 = 0;
				c.bonusAttack = 0;
				c.abbyDagger = 0;
			}
		} else if ((c.playerEquipment[c.playerWeapon] == 5698 || c.playerEquipment[c.playerWeapon] == 19000) && o.poisonDamage <= 0 && Misc.random(3) == 1 && o.playerEquipment[o.playerShield] != 6215 && o.playerEquipment[o.playerShield] != 6217 && o.playerEquipment[o.playerShield] != 6219 && o.playerEquipment[o.playerShield] != 6221 && o.playerEquipment[o.playerShield] != 6223 && o.playerEquipment[o.playerShield] != 6225 && o.playerEquipment[o.playerShield] != 6227 && o.playerEquipment[o.playerShield] != 6229 && o.playerEquipment[o.playerShield] != 6231 && o.playerEquipment[o.playerShield] != 6233 && o.playerEquipment[o.playerShield] != 6235) {
		   o.getPA().appendPoison(13);
		   c.bonusAttack += damage1/3;
		   if(damage1 > 0 || c.abbyDagger == 1) {
				if(c.abbyDagger == 0 && (c.playerEquipment[c.playerWeapon] == 13047 || c.playerEquipment[c.playerWeapon] == 19000)) {
					c.abbyDagger = 1;
					c.delayedDamage2 = (Misc.random(calculateMeleeMaxHit() - 1) + 1);
				} else if(c.playerEquipment[c.playerWeapon] == 13047 || c.playerEquipment[c.playerWeapon] == 19000) {
					damage1 = (Misc.random(calculateMeleeMaxHit() - 1) + 1);
					c.abbyDagger = 0;
				}
			}
		} else {
			c.bonusAttack += damage1/3;
			if(damage1 > 0 || c.abbyDagger == 1) {
				if(c.abbyDagger == 0 && (c.playerEquipment[c.playerWeapon] == 13047 || c.playerEquipment[c.playerWeapon] == 19000)) {
					c.abbyDagger = 1;
					c.delayedDamage2 = (Misc.random(calculateMeleeMaxHit() - 1) + 1);
				} else if(c.playerEquipment[c.playerWeapon] == 13047 || c.playerEquipment[c.playerWeapon] == 19000) {
					damage1 = (Misc.random(calculateMeleeMaxHit() - 1) + 1);
					c.abbyDagger = 0;
				}
			}
		}
		if((o.prayerActive[18] || o.curseActive[9]) && System.currentTimeMillis() - o.protMeleeDelay > 1500 && !veracsEffect) { // if prayer active reduce damage by 40%
			damage1 = (int)damage1 * 40 / 100;
		}
		if (c.maxNextHit) {
			damage1 = calculateMeleeMaxHit();
		}
		if (damage1 > 0 && guthansEffect) {
			c.playerLevel[3] += damage1;
			if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]))
				c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
			c.getPA().refreshSkill(3);
			o.gfx0(398);		
		}
		if (c.ssSpec && damageMask == 2) {
			damage1 = 5 + Misc.random(11);
			c.ssSpec = false;
		}

				Client cl2 = (Client) Server.playerHandler.players[i];
				if (cl2.playerEquipment[5] == 13742) {//Elysian Effect
					if (Misc.random(100) < 75) {
						double damages = damage1;
						double damageDeduction = ((double)damages)/((double)4);
						damage1 = damage1-((int)Math.round(damageDeduction));
					}
				}
				if (cl2.playerEquipment[5] == 13740) {//Divine Effect
					double damages2 = damage1;
					double prayer = cl2.playerLevel[5];
					double possibleDamageDeduction = ((double)damages2)/((double)5);//20% of Damage Inflicted
					double actualDamageDeduction;
					if ((prayer * 2) < possibleDamageDeduction) {
					actualDamageDeduction = (prayer * 2);//Partial Effect(Not enough prayer points)
					} else {
					actualDamageDeduction = possibleDamageDeduction;//Full effect
					}
					double prayerDeduction = ((double)actualDamageDeduction)/((double)2);//Half of the damage deducted
					damage1 = damage1-((int)Math.round(actualDamageDeduction));
					cl2.playerLevel[5] = cl2.playerLevel[5]-((int)Math.round(prayerDeduction));
					cl2.getPA().refreshSkill(5);
				}
		if (Server.playerHandler.players[i].playerLevel[3] - damage1 < 0) { 
			damage1 = Server.playerHandler.players[i].playerLevel[3];
		}
		if (o.vengOn && damage1 > 0)
			appendVengeance(i, damage1);
		if (damage1 > 0)
			applyRecoil(damage1, i);
		switch(c.specEffect) {
			case 1: // dragon scimmy special
			if(damage > 0) {
				if(o.prayerActive[16] || o.prayerActive[17] || o.prayerActive[18] || o.curseActive[7] || o.curseActive[8] || o.curseActive[9]) {
					o.headIcon = -1;
					o.getPA().sendFrame36(c.PRAYER_GLOW[16], 0);
					o.getPA().sendFrame36(c.PRAYER_GLOW[17], 0);
					o.getPA().sendFrame36(c.PRAYER_GLOW[18], 0);	
					o.getPA().sendFrame36(c.CURSE_GLOW[7], 0);
					o.getPA().sendFrame36(c.CURSE_GLOW[8], 0);
					o.getPA().sendFrame36(c.CURSE_GLOW[9], 0);					
				}
				o.sendMessage("You have been injured!");
				o.stopPrayerDelay = System.currentTimeMillis();
				o.prayerActive[16] = false;
				o.prayerActive[17] = false;
				o.prayerActive[18] = false;
				o.curseActive[7] = false;
				o.curseActive[8] = false;
				o.curseActive[9] = false;
				o.getPA().requestUpdates();		
			}
			break;
			case 2:
				if (damage1 > 0) {
					if (o.freezeTimer <= 0)
						o.freezeTimer = 30;
					o.gfx0(369);
					o.sendMessage("You have been frozen.");
					o.frozenBy = c.playerId;
					o.stopMovement();
					c.sendMessage("You freeze your enemy.");
				}		
			break;
			case 3:
				if (damage1 > 0) {
					o.playerLevel[1] -= damage1;
					o.sendMessage("Your defence is weakened.");
					if (o.playerLevel[1] < 1)
						o.playerLevel[1] = 1;
					o.getPA().refreshSkill(1);
				}
			break;
			case 4:
				if (damage1 > 0) {
					if (c.playerLevel[3] + damage1 > c.getLevelForXP(c.playerXP[3]))
						if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]));
						else 
						c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
					else 
						c.playerLevel[3] += damage1;
					c.getPA().refreshSkill(3);
				}
			break;
			case 5:
				c.clawDelay = 2;
			break;
			
		}
		c.specEffect = 0;
		if(c.fightMode == 3) {
			c.getPA().addSkillXP((damage1*Config.MELEE_EXP_RATE/3), 0); 
			c.getPA().addSkillXP((damage1*Config.MELEE_EXP_RATE/3), 1);
			c.getPA().addSkillXP((damage1*Config.MELEE_EXP_RATE/3), 2); 				
			c.getPA().addSkillXP((damage1*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(0);
			c.getPA().refreshSkill(1);
			c.getPA().refreshSkill(2);
			c.getPA().refreshSkill(3);
		} else {
			c.getPA().addSkillXP((damage1*Config.MELEE_EXP_RATE), c.fightMode); 
			c.getPA().addSkillXP((damage1*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(c.fightMode);
			c.getPA().refreshSkill(3);
		}
		Server.playerHandler.players[i].logoutDelay = System.currentTimeMillis();
		Server.playerHandler.players[i].underAttackBy = c.playerId;
		Server.playerHandler.players[i].killerId = c.playerId;	
		Server.playerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
		if (c.killedBy != Server.playerHandler.players[i].playerId)
			c.totalPlayerDamageDealt = 0;
		c.killedBy = Server.playerHandler.players[i].playerId;
		applySmite(i, damage1);
		switch(damageMask) {
			case 1:
			/*if (!Server.playerHandler.players[i].getHitUpdateRequired()){
				Server.playerHandler.players[i].setHitDiff(damage);
				Server.playerHandler.players[i].setHitUpdateRequired(true);
			} else {
				Server.playerHandler.players[i].setHitDiff2(damage);
				Server.playerHandler.players[i].setHitUpdateRequired2(true);			
			}*/
			//Server.playerHandler.players[i].playerLevel[3] -= damage1;
			Server.playerHandler.players[i].dealDamage(damage1);
			Server.playerHandler.players[i].damageTaken[c.playerId] += damage1;
			c.totalPlayerDamageDealt += damage1;
			Server.playerHandler.players[i].updateRequired = true;
			o.getPA().refreshSkill(3);
			break;
		
			case 2:
			/*if (!Server.playerHandler.players[i].getHitUpdateRequired2()){
				Server.playerHandler.players[i].setHitDiff2(damage);
				Server.playerHandler.players[i].setHitUpdateRequired2(true);
			} else {
				Server.playerHandler.players[i].setHitDiff(damage);
				Server.playerHandler.players[i].setHitUpdateRequired(true);			
			}*/
			//Server.playerHandler.players[i].playerLevel[3] -= damage1;
			Server.playerHandler.players[i].dealDamage(damage1);
			Server.playerHandler.players[i].damageTaken[c.playerId] += damage1;
			c.totalPlayerDamageDealt += damage1;
			Server.playerHandler.players[i].updateRequired = true;	
			c.doubleHit = false;
			o.getPA().refreshSkill(3);
			break;			
		}
		Server.playerHandler.players[i].handleHitMask(damage1);
	}
	
	public boolean multis() {
		switch (c.MAGIC_SPELLS[c.oldSpellId][0]) {
			case 1171:
			case 12891:
			case 12881:
			case 13011:
			case 13023:
			case 12919: // blood spells
			case 12929:
			case 12963:
			case 12975:
			return true;
		}
		return false;
	
	}
	public void appendMultiBarrage(int playerId, boolean splashed) {
		if (Server.playerHandler.players[playerId] != null) {
			Client c2 = (Client)Server.playerHandler.players[playerId];
			if (c2.isDead || c2.respawnTimer > 0)
				return;
			if (checkMultiBarrageReqs(playerId)) {
				c.barrageCount++;
				if (Misc.random(mageAtk()) > Misc.random(mageDef()) && !c.magicFailed) {
					if(getEndGfxHeight() == 100){ // end GFX
						c2.gfx100(c.MAGIC_SPELLS[c.oldSpellId][5]);
					} else {
						c2.gfx0(c.MAGIC_SPELLS[c.oldSpellId][5]);
					}
					int damage = Misc.random(c.MAGIC_SPELLS[c.oldSpellId][6]);
					if (c2.prayerActive[12]) {
						damage *= (int)(.60);
					}
					if (c2.playerLevel[3] - damage < 0) {
						damage = c2.playerLevel[3];					
					}
					c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE), 6); 
					c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE/3), 3);
					//Server.playerHandler.players[playerId].setHitDiff(damage);
					//Server.playerHandler.players[playerId].setHitUpdateRequired(true);
					Server.playerHandler.players[playerId].handleHitMask(damage);
					//Server.playerHandler.players[playerId].playerLevel[3] -= damage;
					Server.playerHandler.players[playerId].dealDamage(damage);
					Server.playerHandler.players[playerId].damageTaken[c.playerId] += damage;
					c2.getPA().refreshSkill(3);
					c.totalPlayerDamageDealt += damage;
					multiSpellEffect(playerId, damage);
				} else {
					c2.gfx100(85);
				}			
			}		
		}	
	}
	
	public void multiSpellEffect(int playerId, int damage) {					
		switch(c.MAGIC_SPELLS[c.oldSpellId][0]) {
			case 13011:
			case 13023:
			if(System.currentTimeMillis() - Server.playerHandler.players[playerId].reduceStat > 35000) {
				Server.playerHandler.players[playerId].reduceStat = System.currentTimeMillis();
				Server.playerHandler.players[playerId].playerLevel[0] -= ((Server.playerHandler.players[playerId].getLevelForXP(Server.playerHandler.players[playerId].playerXP[0]) * 10) / 100);
			}	
			break;
			case 12919: // blood spells
			case 12929:
				int heal = (int)(damage / 4);
				if(c.playerLevel[3] + heal >= c.getPA().getLevelForXP(c.playerXP[3])) {
					c.playerLevel[3] = c.getPA().getLevelForXP(c.playerXP[3]);
				} else {
					c.playerLevel[3] += heal;
				}
				c.getPA().refreshSkill(3);
			break;
			case 12891:
			case 12881:
			case 1191:
				if (Server.playerHandler.players[playerId].freezeTimer < -5) {
					Server.playerHandler.players[playerId].freezeTimer = getFreezeTime();
					//Server.playerHandler.players[playerId].stopMovement();
				}
			break;
		}	
	}
	public void applyPlayerClawDamage(int i, int damageMask, int damage){
		Client o = (Client) Server.playerHandler.players[i];
		if(o == null) {
			return;
		}

		c.previousDamage = damage;
		boolean veracsEffect = false;
		boolean guthansEffect = false;
		if (c.getPA().fullVeracs()) {
			if (Misc.random(4) == 1) {
				veracsEffect = true;				
			}		
		}
		if (c.getPA().fullGuthans()) {
			if (Misc.random(4) == 1) {
				guthansEffect = true;
			}		
		}
		if (damageMask == 1) {
			damage = c.delayedDamage;
			c.delayedDamage = 0;
		} else {
			damage = c.delayedDamage2;
			c.delayedDamage2 = 0;
		}
		if(Misc.random(o.getCombat().calculateMeleeDefence()) > Misc.random(calculateMeleeAttack()) && !veracsEffect) {
			damage = 0;
			c.bonusAttack = 0;
		} else if ((c.playerEquipment[c.playerWeapon] == 5698 || c.playerEquipment[c.playerWeapon] == 19000) && o.poisonDamage <= 0 && Misc.random(3) == 1 && o.playerEquipment[o.playerShield] != 6215 && o.playerEquipment[o.playerShield] != 6217 && o.playerEquipment[o.playerShield] != 6219 && o.playerEquipment[o.playerShield] != 6221 && o.playerEquipment[o.playerShield] != 6223 && o.playerEquipment[o.playerShield] != 6225 && o.playerEquipment[o.playerShield] != 6227 && o.playerEquipment[o.playerShield] != 6229 && o.playerEquipment[o.playerShield] != 6231 && o.playerEquipment[o.playerShield] != 6233 && o.playerEquipment[o.playerShield] != 6235) {
		   o.getPA().appendPoison(13);
		   c.bonusAttack += damage/3;
		} else {
			c.bonusAttack += damage/3;
		}
		if((o.prayerActive[18] || o.curseActive[9]) && System.currentTimeMillis() - o.protMeleeDelay > 1500 && !veracsEffect) { // if prayer active reduce damage by 40%
			damage = (int)damage * 40 / 100;
		}
		if (c.maxNextHit) {
			damage = calculateMeleeMaxHit();
		}
		if (damage > 0 && guthansEffect) {
			c.playerLevel[3] += damage;
			if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]))
				c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
			c.getPA().refreshSkill(3);
			o.gfx0(398);		
		}
		if (c.ssSpec && damageMask == 2) {
			damage = 5 + Misc.random(11);
			c.ssSpec = false;
		}
		if (Server.playerHandler.players[i].playerLevel[3] - damage < 0) { 
			damage = Server.playerHandler.players[i].playerLevel[3];
		}
		if (o.vengOn && damage > 0)
			appendVengeance(i, damage);
		if (damage > 0)
			applyRecoil(damage, i);
		switch(c.specEffect) {
			case 1: // dragon scimmy special
			if(damage > 0) {
				if(o.prayerActive[16] || o.prayerActive[17] || o.prayerActive[18] || o.curseActive[7] || o.curseActive[8] || o.curseActive[9]) {
					o.headIcon = -1;
					o.getPA().sendFrame36(c.PRAYER_GLOW[16], 0);
					o.getPA().sendFrame36(c.PRAYER_GLOW[17], 0);
					o.getPA().sendFrame36(c.PRAYER_GLOW[18], 0);	
					o.getPA().sendFrame36(c.CURSE_GLOW[7], 0);
					o.getPA().sendFrame36(c.CURSE_GLOW[8], 0);
					o.getPA().sendFrame36(c.CURSE_GLOW[9], 0);					
				}
				o.sendMessage("You have been injured!");
				o.stopPrayerDelay = System.currentTimeMillis();
				o.prayerActive[16] = false;
				o.prayerActive[17] = false;
				o.prayerActive[18] = false;
				o.curseActive[7] = false;
				o.curseActive[8] = false;
				o.curseActive[9] = false;
				o.getPA().requestUpdates();		
			}
			break;
			case 2:
				if (damage > 0) {
					if (o.freezeTimer <= 0)
						o.freezeTimer = 30;
					o.gfx0(369);
					o.sendMessage("You have been frozen.");
					o.frozenBy = c.playerId;
					o.stopMovement();
					c.sendMessage("You freeze your enemy.");
				}		
			break;
			case 3:
				if (damage > 0) {
					o.playerLevel[1] -= damage;
					o.sendMessage("You feel weak.");
					if (o.playerLevel[1] < 1)
						o.playerLevel[1] = 1;
					o.getPA().refreshSkill(1);
				}
			break;
			case 4:
				if (damage > 0) {
					if (c.playerLevel[3] + damage > c.getLevelForXP(c.playerXP[3]))
						if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]));
						else 
						c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
					else if (c.playerEquipment[c.playerWeapon] == 11698)
						c.playerLevel[3] += damage;
					c.getPA().refreshSkill(3);
				}
			break;
			case 5:
			c.clawDelay = 2;
			break;
		}
		c.specEffect = 0;
		if(c.fightMode == 3) {
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 0); 
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 1);
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 2); 				
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(0);
			c.getPA().refreshSkill(1);
			c.getPA().refreshSkill(2);
			c.getPA().refreshSkill(3);
		} else {
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE), c.fightMode); 
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(c.fightMode);
			c.getPA().refreshSkill(3);
		}
		Server.playerHandler.players[i].logoutDelay = System.currentTimeMillis();
		Server.playerHandler.players[i].underAttackBy = c.playerId;
		Server.playerHandler.players[i].killerId = c.playerId;	
		Server.playerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
		if (c.killedBy != Server.playerHandler.players[i].playerId)
			c.totalPlayerDamageDealt = 0;
		c.killedBy = Server.playerHandler.players[i].playerId;
		applySmite(i, damage);
		if(c.playerRights >= 3)
		c.sendMessage("@red@Smited");
		switch(damageMask) {
			case 1:
			/*if (!Server.playerHandler.players[i].getHitUpdateRequired()){
				Server.playerHandler.players[i].setHitDiff(damage);
				Server.playerHandler.players[i].setHitUpdateRequired(true);
			} else {
				Server.playerHandler.players[i].setHitDiff2(damage);
				Server.playerHandler.players[i].setHitUpdateRequired2(true);			
			}*/
			//Server.playerHandler.players[i].playerLevel[3] -= damage;
			Server.playerHandler.players[i].dealDamage(damage);
			Server.playerHandler.players[i].damageTaken[c.playerId] += damage;
			c.totalPlayerDamageDealt += damage;
			Server.playerHandler.players[i].updateRequired = true;
			o.getPA().refreshSkill(3);
			break;
		
			case 2:
			/*if (!Server.playerHandler.players[i].getHitUpdateRequired2()){
				Server.playerHandler.players[i].setHitDiff2(damage);
				Server.playerHandler.players[i].setHitUpdateRequired2(true);
			} else {
				Server.playerHandler.players[i].setHitDiff(damage);
				Server.playerHandler.players[i].setHitUpdateRequired(true);			
			}*/
			//Server.playerHandler.players[i].playerLevel[3] -= damage;
			Server.playerHandler.players[i].dealDamage(damage);
			Server.playerHandler.players[i].damageTaken[c.playerId] += damage;
			c.totalPlayerDamageDealt += damage;
			Server.playerHandler.players[i].updateRequired = true;	
			c.doubleHit = false;
			o.getPA().refreshSkill(3);
			break;			
		}
		Server.playerHandler.players[i].handleHitMask(damage);
	}
	
	
	public void applyPlayerMeleeDamage(int i, int damageMask){
		Client o = (Client) Server.playerHandler.players[i];
		if(o == null) {
			return;
		}
		int damage = 0;
		boolean veracsEffect = false;
		boolean guthansEffect = false;
		if (c.getPA().fullVeracs()) {
			if (Misc.random(4) == 1) {
				veracsEffect = true;				
			}		
		}
		if (c.getPA().fullGuthans()) {
			if (Misc.random(4) == 1) {
				guthansEffect = true;
			}		
		}
		if (damageMask == 1) {
			damage = c.delayedDamage;
			c.delayedDamage = 0;
		} else {
			damage = c.delayedDamage2;
			c.delayedDamage2 = 0;
		}
		if(Misc.random(o.getCombat().calculateMeleeDefence()) > Misc.random(calculateMeleeAttack()) && !veracsEffect) {
			damage = 0;
			c.bonusAttack = 0;
		} else if ((c.playerEquipment[c.playerWeapon] == 5698 || c.playerEquipment[c.playerWeapon] == 19000) && o.poisonDamage <= 0 && Misc.random(3) == 1 && o.playerEquipment[o.playerShield] != 6215 && o.playerEquipment[o.playerShield] != 6217 && o.playerEquipment[o.playerShield] != 6219 && o.playerEquipment[o.playerShield] != 6221 && o.playerEquipment[o.playerShield] != 6223 && o.playerEquipment[o.playerShield] != 6225 && o.playerEquipment[o.playerShield] != 6227 && o.playerEquipment[o.playerShield] != 6229 && o.playerEquipment[o.playerShield] != 6231 && o.playerEquipment[o.playerShield] != 6233 && o.playerEquipment[o.playerShield] != 6235) {
		   o.getPA().appendPoison(13);
		   c.bonusAttack += damage/3;
		} else if ((c.playerEquipment[c.playerWeapon] == 19113) && o.poisonDamage <= 0 && Misc.random(3) == 1 && o.playerEquipment[o.playerShield] != 6215 && o.playerEquipment[o.playerShield] != 6217 && o.playerEquipment[o.playerShield] != 6219 && o.playerEquipment[o.playerShield] != 6221 && o.playerEquipment[o.playerShield] != 6223 && o.playerEquipment[o.playerShield] != 6225 && o.playerEquipment[o.playerShield] != 6227 && o.playerEquipment[o.playerShield] != 6229 && o.playerEquipment[o.playerShield] != 6231 && o.playerEquipment[o.playerShield] != 6233 && o.playerEquipment[o.playerShield] != 6235) {
		   o.getPA().appendPoison(17);
		   c.bonusAttack += damage/3;
		} else {
			c.bonusAttack += damage/3;
		}
		if((o.prayerActive[18] || o.curseActive[9]) && System.currentTimeMillis() - o.protMeleeDelay > 1500 && !veracsEffect) { // if prayer active reduce damage by 40%
			damage = (int)damage * 40 / 100;
		}
		if (c.maxNextHit) {
			damage = calculateMeleeMaxHit();
		}
		if (damage > 0 && guthansEffect) {
			c.playerLevel[3] += damage;
			if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]))
				c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
			c.getPA().refreshSkill(3);
			o.gfx0(398);		
		}
		if (c.ssSpec && damageMask == 2) {
			damage = 5 + Misc.random(11);
			c.ssSpec = false;
		}
		if (Server.playerHandler.players[i].playerLevel[3] - damage < 0) { 
			damage = Server.playerHandler.players[i].playerLevel[3];
		}
		if (o.vengOn && damage > 0)
			appendVengeance(i, damage);
		if (damage > 0)
			applyRecoil(damage, i);
		switch(c.specEffect) {
			case 1: // dragon scimmy special
			if(damage > 0) {
				if(o.prayerActive[16] || o.prayerActive[17] || o.prayerActive[18] || o.curseActive[7] || o.curseActive[8] || o.curseActive[9]) {
					o.headIcon = -1;
					o.getPA().sendFrame36(c.PRAYER_GLOW[16], 0);
					o.getPA().sendFrame36(c.PRAYER_GLOW[17], 0);
					o.getPA().sendFrame36(c.PRAYER_GLOW[18], 0);	
					o.getPA().sendFrame36(c.CURSE_GLOW[7], 0);
					o.getPA().sendFrame36(c.CURSE_GLOW[8], 0);
					o.getPA().sendFrame36(c.CURSE_GLOW[9], 0);					
				}
				o.sendMessage("You have been injured!");
				o.stopPrayerDelay = System.currentTimeMillis();
				o.prayerActive[16] = false;
				o.prayerActive[17] = false;
				o.prayerActive[18] = false;
				o.curseActive[7] = false;
				o.curseActive[8] = false;
				o.curseActive[9] = false;
				o.getPA().requestUpdates();
			}
			break;
			case 2:
				if (damage > 0) {
					if (o.freezeTimer <= 0)
						o.freezeTimer = 30;
					o.gfx0(369);
					o.sendMessage("You have been frozen.");
					o.frozenBy = c.playerId;
					o.stopMovement();
					c.sendMessage("You freeze your enemy.");
				}		
			break;
			case 3:
				if (damage > 0) {
					o.playerLevel[1] -= damage;
					o.sendMessage("Your defence is weakened.");
					if (o.playerLevel[1] < 1)
						o.playerLevel[1] = 1;
					o.getPA().refreshSkill(1);
				}
			break;
			case 4:
				if (damage > 0) {
					if (c.playerLevel[3] + damage > c.getLevelForXP(c.playerXP[3]))
						if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]));
						else 
						c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
					else 
						c.playerLevel[3] += damage;
					c.getPA().refreshSkill(3);
				}
			break;
			case 5:
			c.clawDelay = 2;
			break;
		}
		c.specEffect = 0;
		if(c.fightMode == 3) {
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 0); 
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 1);
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 2); 				
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(0);
			c.getPA().refreshSkill(1);
			c.getPA().refreshSkill(2);
			c.getPA().refreshSkill(3);
		} else {
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE), c.fightMode); 
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(c.fightMode);
			c.getPA().refreshSkill(3);
		}
		Server.playerHandler.players[i].logoutDelay = System.currentTimeMillis();
		Server.playerHandler.players[i].underAttackBy = c.playerId;
		Server.playerHandler.players[i].killerId = c.playerId;	
		Server.playerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
		if (c.killedBy != Server.playerHandler.players[i].playerId)
			c.totalPlayerDamageDealt = 0;
		c.killedBy = Server.playerHandler.players[i].playerId;
		applySmite(i, damage);
		switch(damageMask) {
			case 1:
			/*if (!Server.playerHandler.players[i].getHitUpdateRequired()){
				Server.playerHandler.players[i].setHitDiff(damage);
				Server.playerHandler.players[i].setHitUpdateRequired(true);
			} else {
				Server.playerHandler.players[i].setHitDiff2(damage);
				Server.playerHandler.players[i].setHitUpdateRequired2(true);			
			}*/
			//Server.playerHandler.players[i].playerLevel[3] -= damage;
			Server.playerHandler.players[i].dealDamage(damage);
			Server.playerHandler.players[i].damageTaken[c.playerId] += damage;
			c.totalPlayerDamageDealt += damage;
			Server.playerHandler.players[i].updateRequired = true;
			o.getPA().refreshSkill(3);
			break;
		
			case 2:
			/*if (!Server.playerHandler.players[i].getHitUpdateRequired2()){
				Server.playerHandler.players[i].setHitDiff2(damage);
				Server.playerHandler.players[i].setHitUpdateRequired2(true);
			} else {
				Server.playerHandler.players[i].setHitDiff(damage);
				Server.playerHandler.players[i].setHitUpdateRequired(true);			
			}*/
			//Server.playerHandler.players[i].playerLevel[3] -= damage;
			Server.playerHandler.players[i].dealDamage(damage);
			Server.playerHandler.players[i].damageTaken[c.playerId] += damage;
			c.totalPlayerDamageDealt += damage;
			Server.playerHandler.players[i].updateRequired = true;	
			c.doubleHit = false;
			o.getPA().refreshSkill(3);
			break;			
		}
		Server.playerHandler.players[i].handleHitMask(damage);
	}
	
	public void applySmite(int index, int damage) {
		if (!c.prayerActive[23] && !c.curseActive[18])
			return;
		if (damage <= 0)
			return;
		if (Server.playerHandler.players[index] != null) { 
			Client c2 = (Client)Server.playerHandler.players[index];
			if(c.curseActive[18] && !c.prayerActive[23] && c.playerLevel[3] <= 99) {
						int heal = (int)(damage/5);
						if(c.playerLevel[3] + heal >= c.getPA().getLevelForXP(c.playerXP[3])) {
							c.playerLevel[3] = c.getPA().getLevelForXP(c.playerXP[3]);
						} else {
							c.playerLevel[3] += heal;
						}
						c.getPA().refreshSkill(3);
			}
			if(c.worshippedGod == 2 && c.godReputation2 >= 250){
				c2.playerLevel[5] -= (int)(damage/4 + Misc.random(damage/5));
			} else 
				c2.playerLevel[5] -= (int)(damage/4);
				
			if (c2.playerLevel[5] <= 0) {
				c2.playerLevel[5] = 0;
				c2.getCombat().resetPrayers();
			}
			c2.getPA().refreshSkill(5);
		}
	
	}
	
		

	public void fireProjectilePlayer() {
		if(c.oldPlayerIndex > 0) {
			if(Server.playerHandler.players[c.oldPlayerIndex] != null) {
				c.projectileStage = 2;
				int pX = c.getX();
				int pY = c.getY();
				int oX = Server.playerHandler.players[c.oldPlayerIndex].getX();
				int oY = Server.playerHandler.players[c.oldPlayerIndex].getY();
				int offX = (pY - oY)* -1;
				int offY = (pX - oX)* -1;	
				if (!c.msbSpec) {
					if(c.cannonSpec == 1){
						c.getPA().createPlayersProjectile2(pX, pY, offX, offY, 50, 100, getRangeProjectileGFX(), 43, 31, - c.oldPlayerIndex - 1, getStartDelay(), 10);
						c.getPA().createPlayersProjectile2(pX, pY, offX, offY, 50, 70, getRangeProjectileGFX(), 43, 31, - c.oldPlayerIndex - 1, getStartDelay(), 5);
					} else {
						c.getPA().createPlayersProjectile(pX, pY, offX, offY, 50, getProjectileSpeed(), getRangeProjectileGFX(), 43, 31, - c.oldPlayerIndex - 1, getStartDelay());
					}
				} else if (c.msbSpec) {
					c.getPA().createPlayersProjectile2(pX, pY, offX, offY, 50, getProjectileSpeed(), getRangeProjectileGFX(), 43, 31, - c.oldPlayerIndex - 1, getStartDelay(), 10);
					c.msbSpec = false;
				}
				if (usingDbow())
					c.getPA().createPlayersProjectile2(pX, pY, offX, offY, 50, getProjectileSpeed(), getRangeProjectileGFX(), 60, 31, - c.oldPlayerIndex - 1, getStartDelay(), 35);
			}
		}
	}
	
	public boolean usingDbow() {
		return c.playerEquipment[c.playerWeapon] == 11235;
	}
	
	
	

	
	/**Prayer**/
		
	public void activatePrayer(int i) {
		if(i == 10 && c.isInHighRiskPK()){
		c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
		c.sendMessage("This cannot be used in high-risk PK.");
		return;
		}
		if(i == 10 && c.inNoProtectItem()){
		c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
		c.sendMessage("This cannot be used in high-risk PK.");
		return;
		}
		

		if (i == 24 && c.playerLevel[1] < 60) {
			c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
			c.sendMessage("You need 60 defence and 65 prayer to use this.");
			return;
		}
		if (i == 25 && c.playerLevel[1] < 28) {
			c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
			c.sendMessage("You need defence level of 28 to use this!");
			return;
		}
		int[] defPray = {0,5,13,24,25};
		int[] strPray = {1,6,14,24,25};
		int[] atkPray = {2,7,15,24,25};
		int[] rangePray = {3,11,19};
		int[] magePray = {4,12,20};

		if(c.playerLevel[5] > 0 || !Config.PRAYER_POINTS_REQUIRED){
			if(c.getPA().getLevelForXP(c.playerXP[5]) >= c.PRAYER_LEVEL_REQUIRED[i] || !Config.PRAYER_LEVEL_REQUIRED) {
				boolean headIcon = false;
				switch(i) {
					case 0:
					case 5:
					case 13:
					if(c.prayerActive[i] == false) {
						for (int j = 0; j < defPray.length; j++) {
							if (defPray[j] != i) {
								c.prayerActive[defPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[defPray[j]], 0);
							}								
						}
					}
					break;
					
					case 1:
					case 6:
					case 14:
					if(c.prayerActive[i] == false) {
						for (int j = 0; j < strPray.length; j++) {
							if (strPray[j] != i) {
								c.prayerActive[strPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[strPray[j]], 0);
							}								
						}
						for (int j = 0; j < rangePray.length; j++) {
							if (rangePray[j] != i) {
								c.prayerActive[rangePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[rangePray[j]], 0);
							}								
						}
						for (int j = 0; j < magePray.length; j++) {
							if (magePray[j] != i) {
								c.prayerActive[magePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[magePray[j]], 0);
							}								
						}
					}
					break;
					
					case 2:
					case 7:
					case 15:
					if(c.prayerActive[i] == false) {
						for (int j = 0; j < atkPray.length; j++) {
							if (atkPray[j] != i) {
								c.prayerActive[atkPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[atkPray[j]], 0);
							}								
						}
						for (int j = 0; j < rangePray.length; j++) {
							if (rangePray[j] != i) {
								c.prayerActive[rangePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[rangePray[j]], 0);
							}								
						}
						for (int j = 0; j < magePray.length; j++) {
							if (magePray[j] != i) {
								c.prayerActive[magePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[magePray[j]], 0);
							}								
						}
					}
					break;
					
					case 3://range prays
					case 11:
					case 19:
					if(c.prayerActive[i] == false) {
						for (int j = 0; j < atkPray.length; j++) {
							if (atkPray[j] != i) {
								c.prayerActive[atkPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[atkPray[j]], 0);
							}								
						}
						for (int j = 0; j < strPray.length; j++) {
							if (strPray[j] != i) {
								c.prayerActive[strPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[strPray[j]], 0);
							}								
						}
						for (int j = 0; j < rangePray.length; j++) {
							if (rangePray[j] != i) {
								c.prayerActive[rangePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[rangePray[j]], 0);
							}								
						}
						for (int j = 0; j < magePray.length; j++) {
							if (magePray[j] != i) {
								c.prayerActive[magePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[magePray[j]], 0);
							}								
						}
					}
					break;
					case 4:
					case 12:
					case 20:
					if(c.prayerActive[i] == false) {
						for (int j = 0; j < atkPray.length; j++) {
							if (atkPray[j] != i) {
								c.prayerActive[atkPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[atkPray[j]], 0);
							}								
						}
						for (int j = 0; j < strPray.length; j++) {
							if (strPray[j] != i) {
								c.prayerActive[strPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[strPray[j]], 0);
							}								
						}
						for (int j = 0; j < rangePray.length; j++) {
							if (rangePray[j] != i) {
								c.prayerActive[rangePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[rangePray[j]], 0);
							}								
						}
						for (int j = 0; j < magePray.length; j++) {
							if (magePray[j] != i) {
								c.prayerActive[magePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[magePray[j]], 0);
							}								
						}
					}
					break;
					case 10:
						c.lastProtItem = System.currentTimeMillis();
					break;
					

					case 16:					
					case 17:
					case 18:
					if(System.currentTimeMillis() - c.stopPrayerDelay < 5000) {
						c.sendMessage("You have been injured and can't use this prayer!");
						c.getPA().sendFrame36(c.PRAYER_GLOW[16], 0);
						c.getPA().sendFrame36(c.PRAYER_GLOW[17], 0);
						c.getPA().sendFrame36(c.PRAYER_GLOW[18], 0);
						return;
					}
					if (i == 16)
						c.protMageDelay = System.currentTimeMillis();
					else if (i == 17)
						c.protRangeDelay = System.currentTimeMillis();
					else if (i == 18)
						c.protMeleeDelay = System.currentTimeMillis();
					case 21:
					case 22:
					case 23:
					headIcon = true;		
					for(int p = 16; p < 24; p++) {
						if(i != p && p != 19 && p != 20) {
							c.prayerActive[p] = false;
							c.getPA().sendFrame36(c.PRAYER_GLOW[p], 0);
						}
					}
					break;
					case 24:
					case 25:
					if (c.prayerActive[i] == false) {
						for (int j = 0; j < atkPray.length; j++) {
							if (atkPray[j] != i) {
								c.prayerActive[atkPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[atkPray[j]], 0);
							}								
						}
						for (int j = 0; j < strPray.length; j++) {
							if (strPray[j] != i) {
								c.prayerActive[strPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[strPray[j]], 0);
							}								
						}
						for (int j = 0; j < rangePray.length; j++) {
							if (rangePray[j] != i) {
								c.prayerActive[rangePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[rangePray[j]], 0);
							}								
						}
						for (int j = 0; j < magePray.length; j++) {
							if (magePray[j] != i) {
								c.prayerActive[magePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[magePray[j]], 0);
							}								
						}
						for (int j = 0; j < defPray.length; j++) {
							if (defPray[j] != i) {
								c.prayerActive[defPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[defPray[j]], 0);
							}								
						}
					}
					break;
				}
				
				if(!headIcon) {
					if(c.prayerActive[i] == false) {
						c.prayerActive[i] = true;
						c.getPA().sendFrame36(c.PRAYER_GLOW[i], 1);					
					} else {
						c.prayerActive[i] = false;
						c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
					}
				} else {
					if(c.prayerActive[i] == false) {
						c.prayerActive[i] = true;
						c.getPA().sendFrame36(c.PRAYER_GLOW[i], 1);
						c.headIcon = c.PRAYER_HEAD_ICONS[i];
						c.getPA().requestUpdates();
					} else {
						c.prayerActive[i] = false;
						c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
						c.headIcon = -1;
						c.getPA().requestUpdates();
					}
				}
			} else {
				c.getPA().sendFrame36(c.PRAYER_GLOW[i],0);
				/*c.getPA().sendFrame126("You need a Prayer level of "+c.PRAYER_LEVEL_REQUIRED[i]+" to use "+c.PRAYER_NAME[i]+".", 357);
				c.getPA().sendFrame126("Click here to continue", 358);
				c.getPA().sendFrame164(356);*/
				c.sendMessage("You need a Prayer level of "+c.PRAYER_LEVEL_REQUIRED[i]+" to use "+c.PRAYER_NAME[i]+"");
			}
		} else {
			c.getPA().sendFrame36(c.PRAYER_GLOW[i],0);
			c.sendMessage("You have run out of prayer points!");
		}	
				
	}
		
	/**
	*Specials
	**/
	
	public void activateSpecial(int weapon, int i){
	Client d = (Client)Server.playerHandler.players[i];
		if(Server.npcHandler.npcs[i] == null && c.npcIndex > 0) {
			return;
		}
		if(Server.playerHandler.players[i] == null && c.playerIndex > 0) {
			return;
		}
		if(c.playerLevel[3] == 0)
			return;
		c.doubleHit = false;
		c.usingClaws = false;
		c.specEffect = 0;
		c.projectileStage = 0;
		c.specMaxHitIncrease = 2;
		c.isUsingSpecial = true;
		if(c.npcIndex > 0) {
			c.oldNpcIndex = i;
		} else if (c.playerIndex > 0){
			c.oldPlayerIndex = i;
			Server.playerHandler.players[i].underAttackBy = c.playerId;
			Server.playerHandler.players[i].logoutDelay = System.currentTimeMillis();
			Server.playerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
			Server.playerHandler.players[i].killerId = c.playerId;
		}
		switch(weapon) {
			
			case 1305: // dragon long
			c.gfx100(248);
			c.startAnimation(1058);
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.specAccuracy = 2.10;
			c.specDamage = 1.20;
			break;

			case 13047: //Abyssal dagger
			case 19000:
			c.gfx100(252);
			c.startAnimation(1062);
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.doubleHit = true;
			c.specAccuracy = 1.25;
			c.specDamage = 0.75;
			c.abbyDagger = 0;
			break;

			case 1215: // dragon daggers
			case 1231:
			case 5680:
			case 5698:
			c.gfx100(252);
			c.startAnimation(1062);
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.doubleHit = true;
			c.specAccuracy = 1.35;
			c.specDamage = 1.05;
			if(c.playerName.equalsIgnoreCase("elvemage")){
				c.specAccuracy = 4.00;
				c.specDamage = 1.15;
			}
			break;
			
			case 13022: // hand cannon
				c.usingBow = true;
				c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase())+1;
				c.projectileStage = 1;
				c.cannonSpec = 1;
				c.specDamage = 0.85;
				c.specAccuracy = 1.20;
				c.startAnimation(2075);
				if (c.fightMode == 2)
					c.attackTimer--;
				if (c.playerIndex > 0)
					fireProjectilePlayer();
				else if (c.npcIndex > 0)
					fireProjectileNpc();
			break;
			
			case 10887: //anchor
				c.startAnimation(5870);
				c.specDamage = 1.25;
				c.specAccuracy = 8;
				c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;			

			case 11730: //Sara sword
			c.gfx100(1224);
			c.startAnimation(7072);
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.doubleHit = true;
			c.ssSpec = true;
			c.specAccuracy = 3;
			c.specDamage = 1.20;
			break;

			case 13045: //Bludgeon
			if(d != null) {
				d.gfx0(78);
			} else if (Server.npcHandler.npcs[i] != null) {
				Server.npcHandler.npcs[i].gfx0(78);
			}
			c.startAnimation(2066);
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.specAccuracy = 3;
			int pMissing = c.getLevelForXP(c.playerXP[5]) - c.playerLevel[5];
			c.specDamage = 1.25 + ((pMissing * .5) / 100);
			break;
			
			case 3757: //fremennik blade
				c.startAnimation(2078);
				c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
				c.specAccuracy = 2.25;
				c.specDamage = 1.25;
			break;
			
			case 13899: // VLS
			c.startAnimation(6502);
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()+1);
			c.specDamage = 1.30;
			c.specAccuracy = 3.00;
			break;
			case 14484: // Dragon claws
				c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
				c.startAnimation(6000);
				c.doubleHit = true;
				c.usingClaws = true;
				c.specAccuracy = 2.25;
				c.specDamage = 1.135;
				if(c.playerName.equalsIgnoreCase("elvemage"))
			c.specAccuracy = 4;
			break;
			
			case 16018:
				int damage = Misc.random(10) + 5;
				if(d != null) {
					if(damage > d.playerLevel[3])
						damage = d.playerLevel[3];
					d.gfx0(78);
				} else if (Server.npcHandler.npcs[i] != null) {
					if(damage > Server.npcHandler.npcs[i].HP)
						damage = Server.npcHandler.npcs[i].HP;
					Server.npcHandler.npcs[i].gfx0(78);
				}
				final int hitDamage = damage;
				final Client c2 = d;
				final int i2 = i;
				c.startAnimation(1658);
				c.specAccuracy = 20;
				c.specDamage = 1.15;
				c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
				CycleEventHandler.getSingleton().addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						if (Server.npcHandler.npcs[i2] != null && Server.npcHandler.npcs[i2].HP > 0) {
							Server.npcHandler.npcs[i2].handleHitMask(hitDamage);
							Server.npcHandler.npcs[i2].hitDiff = hitDamage;
							Server.npcHandler.npcs[i2].HP -= hitDamage;
							Server.npcHandler.npcs[i2].hitUpdateRequired = true;
							container.stop();
						}
					if (c2 != null && c2.playerLevel[3] > 0) {
						c2.handleHitMask(hitDamage);
						c2.dealDamage(hitDamage);
						c2.getPA().refreshSkill(3);
						container.stop();
					}	
						
					}

					@Override
					public void stop() {
						// TODO Auto-generated method stub
						
					}
					
				}, 2);
			break;
			case 19113:
			case 4151: // whip
			if((Server.npcHandler.npcs[i] != null)) {
				Server.npcHandler.npcs[i].gfx100(341);
			} 
if(c.inWild()) {
if(d != null)
d.gfx100(341);
}
			if(Misc.random(1) == 0){
			c.specAccuracy = 20;
			c.specDamage = 1.10;
			} else {
			c.specAccuracy = .25;
			}
			c.startAnimation(1658);
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;
			case 11694: // ags
			c.startAnimation(4304);
			c.specDamage = 1.375;
			c.specAccuracy = 3.00;
			c.gfx0(1222);
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;
			
			case 19780://Korasi
			if (c.playerIndex > 0) {
			c.startAnimation(1058);         
			c.gfx100(1224);
			c.specAccuracy = 8.00;
			//c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			if(d.playerEquipment[d.playerShield] == 6259 || d.playerEquipment[d.playerShield] == 6261 || d.playerEquipment[d.playerShield] == 6263 || d.playerEquipment[d.playerShield] == 6265 || d.playerEquipment[d.playerShield] == 6267 || d.playerEquipment[d.playerShield] == 6269 || d.playerEquipment[d.playerShield] == 6271 || d.playerEquipment[d.playerShield] == 6273 || d.playerEquipment[d.playerShield] == 6275 || d.playerEquipment[d.playerShield] == 6277 || d.playerEquipment[d.playerShield] == 6279)
			   {
			    d.sendMessage("Your shield reduced the Korasi's special by half.");
			    pvpMageDamage(((int) (Misc.random(calculateMeleeMaxHit()) * 1) + (calculateMeleeMaxHit()/2)) / 2);
			    
			   }
			   else
			   {
			    pvpMageDamage((int) (Misc.random(calculateMeleeMaxHit()) * 1) + (calculateMeleeMaxHit()/2));
			   }
			//c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			//c.specEffect = 10;
			}
			if(Server.npcHandler.npcs[i] != null && c.npcIndex > 0) {
				c.gfx100(1224);
				c.specDamage = 2.00;
				c.specAccuracy = 5.00;
				c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			}
			break;			
			case 13902: //statius hammer
			c.startAnimation(6505);
			c.gfx100(1223);
			c.specAccuracy = 2;
			c.specDamage = 1.25;
			c.specEffect = 3;
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;
			case 11700:
				c.startAnimation(4302);		
				c.gfx0(1221);
				c.specAccuracy = 1.25;
				c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
				c.specEffect = 2;
			break;
			
			case 11696:
				c.startAnimation(4301);
				c.gfx0(1223);
				c.specDamage = 1.21;
				c.specAccuracy = 6;
				c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
				c.specEffect = 3;
			break;
			
			case 11698:
				c.startAnimation(4303);
				c.gfx0(1220);
				c.specAccuracy = 2.25;
				c.specEffect = 4;
				c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;
			
			case 1249:
			case 11716:
				c.startAnimation(405);
				c.gfx100(253);
				if (c.playerIndex > 0) {
					Client o = (Client)Server.playerHandler.players[i];
					o.getPA().getSpeared(c.absX, c.absY);
				}	
			break;
			
			case 3204: // d hally
			c.gfx100(282);
			c.startAnimation(1203);
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.specAccuracy = 1.0;
			if(Server.npcHandler.npcs[i] != null && c.npcIndex > 0) {
				//if(!c.goodDistance(c.getX(), c.getY(), Server.npcHandler.npcs[i].getX(), Server.npcHandler.npcs[i].getY(), 1)){
					c.doubleHit = true;
				//}
			}
			if(Server.playerHandler.players[i] != null && c.playerIndex > 0) {
				//if(!c.goodDistance(c.getX(), c.getY(), Server.playerHandler.players[i].getX(),Server.playerHandler.players[i].getY(), 1)){
					c.doubleHit = true;
					c.delayedDamage2 = (int) Misc.random(calculateMeleeMaxHit() / 3);
				//}
			}
			break;
			
			/*case 4153: // maul
			c.startAnimation(1667);
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());// uses handlegmaulplayer
			c.gfx100(340);
			c.specAccuracy = 1;
			break;*/
			
			case 4153: // maul
				c.startAnimation(1667);
				c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());// uses handlegmaulplayer
				c.gfx100(340);
				c.specDamage = .2;
				c.specAccuracy = .7;
				break;
			
			case 4587: // dscimmy
			c.gfx100(347);
			c.specEffect = 1;
			c.startAnimation(1872);
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;
			
			case 1434: // mace
			c.startAnimation(1060);
			c.gfx100(251);
			c.specMaxHitIncrease = 3;
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase())+1;
			c.specDamage = 1.35;
			c.specAccuracy = 1.5;
			break;
			
			case 859: // magic long
			c.usingBow = true;
			c.bowSpecShot = 3;
			c.rangeItemUsed = c.playerEquipment[c.playerArrows];
			c.getItems().deleteArrow();	
			c.lastWeaponUsed = weapon;
			c.startAnimation(426);
			c.gfx100(250);	
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.projectileStage = 1;
			if (c.fightMode == 2)
				c.attackTimer--;
			break;
			
			case 861: // magic short	
			c.usingRangeWeapon = true;
			c.usingBow = true;			
			c.bowSpecShot = 1;
			c.rangeItemUsed = c.playerEquipment[c.playerArrows];
			c.getItems().deleteArrow();	
			c.lastWeaponUsed = weapon;
			c.startAnimation(1074);
			c.hitDelay = 3;
			c.projectileStage = 1;
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			if (c.fightMode == 2)
				c.attackTimer--;
			if (c.playerIndex > 0)
				fireProjectilePlayer();
			else if (c.npcIndex > 0)
				fireProjectileNpc();	
			break;
			
			case 4212:
			c.usingBow = true;			
			c.rangeItemUsed = c.playerEquipment[c.playerArrows];	
			c.lastWeaponUsed = weapon;
			c.gfx0(474);
			c.hitDelay = 3;
			c.projectileStage = 1;
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.attackTimer-=3;
			
			if (c.playerIndex > 0)
				fireProjectilePlayer();
			else if (c.npcIndex > 0)
				fireProjectileNpc();

			if(c.crystalBowSpecTimer >= 0 && c.crystalBowSpecTimer < 60)
			{
				if(c.crystalBowSpecTimer == 0)
					c.sendMessage("@red@You enrage your crystal bow.");
				else if (c.crystalBowSpecTimer > 0)
					c.sendMessage("@red@You enrage your crystal bow for an additional 15 seconds!");
			
				c.crystalBowSpecTimer += 30;
			}
			else if(c.crystalBowSpecTimer >= 60)
				c.sendMessage("@red@You cannot enrage your bow for any longer!");
		
			break;
			
			case 11235: // dark bow	
			case 9705:
			c.usingBow = true;
			c.dbowSpec = true;
			c.rangeItemUsed = c.playerEquipment[c.playerArrows];
			c.getItems().deleteArrow();
			c.getItems().deleteArrow();
			c.lastWeaponUsed = weapon;
			c.hitDelay = 3;
			c.startAnimation(426);
			c.projectileStage = 1;
			c.gfx100(getRangeStartGFX());
			c.hitDelay = getHitDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			if (c.fightMode == 2)
				c.attackTimer--;
			if (c.playerIndex > 0)
				fireProjectilePlayer();
			else if (c.npcIndex > 0)
				fireProjectileNpc();
			c.specAccuracy = 2.5;
			c.specDamage = 1.50;
			break;
		}
		if(c.playerEquipment[c.playerWeapon] == 11235){
		c.delayedDamage = Misc.random(rangeMaxHit());
		c.delayedDamage2 = Misc.random(rangeMaxHit());
		}
		if(c.playerEquipment[c.playerWeapon] != 11235){
				c.delayedDamage = Misc.random(calculateMeleeMaxHit());
				c.delayedDamage2 = Misc.random(calculateMeleeMaxHit());
		}
		c.usingSpecial = false;
		c.getItems().updateSpecialBar();
	}

private static final int[][] ORANGESHIELDS  = {{6237, 6239, 9}, {6239, 6241, 8}, {6241, 6243, 7}, {6243, 6245, 6}, {6245, 6247, 5}, {6247, 6249, 4}, {6249, 6251, 3}, {6251, 6253, 2}, {6253, 6255, 1}, {6255, 6257, 0}};
	private static final int[][] WHITESHIELDS  = {{6259, 6261, 9}, {6261, 6263, 8}, {6263, 6265, 7}, {6265, 6267, 6}, {6267, 6269, 5}, {6269, 6271, 4}, {6271, 6273, 3}, {6273, 6275, 2}, {6275, 6277, 1}, {6277, 6279, 0}};
private static final int[][] GREENSHIELDS  = {{6215, 6217, 9}, {6217, 6219, 8}, {6219, 6221, 7}, {6221, 6223, 6}, {6223, 6225, 5}, {6225, 6227, 4}, {6227, 6229, 3}, {6229, 6231, 2}, {6231, 6233, 1}, {6233, 6235, 0}};

public static void orangeCharges(Client c) 
	{
		for (int i = 0; i < ORANGESHIELDS.length; i++) 
		{
			if (c.itemUsing == ORANGESHIELDS[i][0]) 
			{
				c.playerEquipment[c.playerShield] = ORANGESHIELDS[i][1];
				if (ORANGESHIELDS[i][2] > 1) 
				{
					c.sendMessage("You have "+ORANGESHIELDS[i][2]+" charges left.");
				} else {
					c.sendMessage("You have "+ORANGESHIELDS[i][2]+" charges left.");
				}
			}
		}
		c.getItems().updateSlot(c.playerShield);
		c.itemUsing = -1;
	}




	public static void greenCharges(Client c) 
	{
		for (int i = 0; i < GREENSHIELDS.length; i++) 
		{
			if (c.itemUsing == GREENSHIELDS[i][0]) 
			{
				c.playerEquipment[c.playerShield] = GREENSHIELDS[i][1];
				if (GREENSHIELDS[i][2] > 1) 
				{
					c.sendMessage("You have "+GREENSHIELDS[i][2]+" charges left.");
				} else {
					c.sendMessage("You have "+GREENSHIELDS[i][2]+" charges left.");
				}
			}
		}
		c.getItems().updateSlot(c.playerShield);
		c.itemUsing = -1;
	}
public static void whiteCharges(Client c) 
	{
		for (int i = 0; i < WHITESHIELDS.length; i++) 
		{
			if (c.itemUsing == WHITESHIELDS[i][0]) 
			{
				c.playerEquipment[c.playerShield] = WHITESHIELDS[i][1];
				if (WHITESHIELDS[i][2] > 1) 
				{
					c.sendMessage("You have "+WHITESHIELDS[i][2]+" charges left.");
				} else {
					c.sendMessage("You have "+WHITESHIELDS[i][2]+" charges left.");
				}
			}
		}
		c.getItems().updateSlot(c.playerShield);
		c.itemUsing = -1;
	}

	public void handleGreenBroodooShield()
	{
		if(!c.inStakeArena()) {
			if(c.playerEquipment[c.playerShield] == 6215 || c.playerEquipment[c.playerShield] == 6217 || c.playerEquipment[c.playerShield] == 6219 || c.playerEquipment[c.playerShield] == 6221 || c.playerEquipment[c.playerShield] == 6223 || c.playerEquipment[c.playerShield] == 6225 || c.playerEquipment[c.playerShield] == 6227 || c.playerEquipment[c.playerShield] == 6229 || c.playerEquipment[c.playerShield] == 6231 || c.playerEquipment[c.playerShield] == 6233)
			{
			if (System.currentTimeMillis() - c.greenShieldDelay > 30000) 
				{
			   if (c.playerIndex > 0 && Server.playerHandler.players[c.playerIndex] != null) 
			   {
			   	Client o = (Client)Server.playerHandler.players[c.playerIndex];
			    o.getPA().appendPoison(22);
			    greenCharges(c);
			    c.greenShieldDelay = System.currentTimeMillis();
			    c.sendMessage("Your opponent has been poisoned!");
			   }
			} else {
				c.sendMessage("@red@Your shield has not finished recharging.");
			}
			}
		} else {
			c.sendMessage("You can't use this here!");
		}
		
	}

	public void handleWhiteBroodooShield()
	{
		if(!c.inStakeArena()) {
			if(c.playerEquipment[c.playerShield] == 6259 || c.playerEquipment[c.playerShield] == 6261 || c.playerEquipment[c.playerShield] == 6263 || c.playerEquipment[c.playerShield] == 6265 || c.playerEquipment[c.playerShield] == 6267 || c.playerEquipment[c.playerShield] == 6269 || c.playerEquipment[c.playerShield] == 6271 || c.playerEquipment[c.playerShield] == 6273 || c.playerEquipment[c.playerShield] == 6275 || c.playerEquipment[c.playerShield] == 6277)
			{
				if (System.currentTimeMillis() - c.greenShieldDelay > 45000) 
				{
					if (c.playerIndex > 0 && Server.playerHandler.players[c.playerIndex] != null) {
							Client o = (Client)Server.playerHandler.players[c.playerIndex];
						int damage = Misc.random(15); //currently does a random amount of 15 damage, but its lowest damage is 10.
						o.gfx0(369);
						Server.playerHandler.players[c.playerIndex].playerLevel[3] -= damage;
						Server.playerHandler.players[c.playerIndex].hitDiff2 = damage;
						Server.playerHandler.players[c.playerIndex].hitUpdateRequired2 = true;
						Server.playerHandler.players[c.playerIndex].updateRequired = true;
						((Client) Server.playerHandler.players[c.playerIndex]).getPA().requestUpdates();
						c.greenShieldDelay = System.currentTimeMillis();		
						whiteCharges(c);
						o.freezeTimer = 60;
						o.sendMessage("You have been frozen.");
						
					} else {
						c.sendMessage("@red@I should be in combat before using this.");
					}
				} else {
					c.sendMessage("@red@My shield hasn't finished recharging yet.");
				}
			}
		} else {
			c.sendMessage("You can't use this here!");
		}
		
	}
	
	public void handleOrangeBroodooShield()
	{
		if(!c.inStakeArena()) {
			if(c.playerEquipment[c.playerShield] == 6237 || c.playerEquipment[c.playerShield] == 6239 || c.playerEquipment[c.playerShield] == 6241 || c.playerEquipment[c.playerShield] == 6243 || c.playerEquipment[c.playerShield] == 6245 || c.playerEquipment[c.playerShield] == 6247 || c.playerEquipment[c.playerShield] == 6249 || c.playerEquipment[c.playerShield] == 6251 || c.playerEquipment[c.playerShield] == 6253 || c.playerEquipment[c.playerShield] == 6255)
			{
				if (System.currentTimeMillis() - c.greenShieldDelay > 45000) 
				{
					if (c.playerIndex > 0 && Server.playerHandler.players[c.playerIndex] != null) {
							Client o = (Client)Server.playerHandler.players[c.playerIndex];
							int damage = Misc.random(15) + 10; //currently does a random amount of 25 damage, but its lowest damage is 10.
							o.gfx0(451);
							Server.playerHandler.players[c.playerIndex].playerLevel[3] -= damage;
							Server.playerHandler.players[c.playerIndex].hitDiff2 = damage;
							Server.playerHandler.players[c.playerIndex].hitUpdateRequired2 = true;
							Server.playerHandler.players[c.playerIndex].updateRequired = true;
							((Client) Server.playerHandler.players[c.playerIndex]).getPA().requestUpdates();
							c.greenShieldDelay = System.currentTimeMillis();		
							orangeCharges(c);
					} else {
						c.sendMessage("@red@I should be in combat before using this.");
					}
				} else {
					c.sendMessage("@red@My shield hasn't finished recharging yet.");
				}
			}
		} else {
			c.sendMessage("You can't use this here!");
		}
	}

		public void handleDfs() {

		if(!c.inStakeArena()) {
			if(c.playerEquipment[c.playerShield] == 11284){
				if (System.currentTimeMillis() - c.dfsDelay > 30000) {
				

					if (c.playerIndex > 0 && Server.playerHandler.players[c.playerIndex] != null) {
							Client o = (Client)Server.playerHandler.players[c.playerIndex];
						int damage = Misc.random(10);//the damage! This should be good -james
						if(damage < 2)
							damage = 2;
						//c.startAnimation(2836);
						if(o.getPA().antiFire() == 1){
						damage = (int)((double)damage*.75);
						o.sendMessage("@red@You are slightly protected from the dragonfire shield.");
						}
						if(o.getPA().antiFire() == 2){
						damage = (int)((double)damage*.5);
						o.sendMessage("@red@The dragonfire shield is not very effective against you.");
						}
						Server.playerHandler.players[c.playerIndex].playerLevel[3] -= damage;
						Server.playerHandler.players[c.playerIndex].hitDiff2 = damage;
						Server.playerHandler.players[c.playerIndex].hitUpdateRequired2 = true;
						Server.playerHandler.players[c.playerIndex].updateRequired = true;
						((Client) Server.playerHandler.players[c.playerIndex]).getPA().requestUpdates();
						c.dfsDelay = System.currentTimeMillis();						
					} else {
						c.sendMessage("@red@I should be in combat before using this.");
					}
				} else {
					c.sendMessage("@red@My shield hasn't finished recharging yet.");
				}
				}
		} else {
			c.sendMessage("You can't use this here!");
		}
		
	}
	
	public void handleFremmyShield() {
		if(!c.inStakeArena()) {
			if(c.playerEquipment[c.playerShield] == 3758){
				if (System.currentTimeMillis() - c.dfsDelay > 25000) {
				

					if (c.playerIndex > 0 && Server.playerHandler.players[c.playerIndex] != null) {
							Client o = (Client)Server.playerHandler.players[c.playerIndex];
						int damage = Misc.random(11);
						if(damage < 3)
							damage = 3;
						c.startAnimation(439);
						Server.playerHandler.players[c.playerIndex].playerLevel[3] -= damage;
						Server.playerHandler.players[c.playerIndex].hitDiff2 = damage;
						Server.playerHandler.players[c.playerIndex].hitUpdateRequired2 = true;
						Server.playerHandler.players[c.playerIndex].updateRequired = true;
						((Client) Server.playerHandler.players[c.playerIndex]).getPA().requestUpdates();
						c.dfsDelay = System.currentTimeMillis();						
					} else {
						c.sendMessage("@red@I should be in combat before using this.");
					}
				} else {
					c.sendMessage("@red@My shield hasn't finished recharging yet.");
				}
				}
		} else {
			c.sendMessage("You can't use this here!");
		}
		
	}
	
	public boolean checkSpecAmount(int weapon) {
		double ring = 1.0;
			if(c.playerEquipment[c.playerRing] == 773)
			{
			ring = .8;
			}
		switch(weapon) {
			case 1249:
			case 11716:
			case 1215:
			case 1231:
			case 5680:
			case 5698:
			case 1305:
			case 1434:
			case 15006:
			case 13899:
			if(c.specAmount >= 2.5 * ring) {
                c.specAmount -= 2.5 * ring;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;
			
			case 13047:
			case 19000:
			case 13045:
			case 4153://gmaul combos were way too op
			if(c.specAmount >= 5 * ring) {
                c.specAmount -= 5 * ring;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;
			case 19113:
			case 4151:
			case 11694:
			case 14484:
			case 11698:
			case 13022:
			case 10887:
			case 15027:
			case 16018:
			
			if(c.specAmount >= 5 * ring) {
                c.specAmount -= 5 * ring;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;
			
			case 3204:
			if(c.specAmount >= 3 * ring) {
                c.specAmount -= 3 * ring;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;
			
			case 13902:
			case 3757:
			if(c.specAmount >= 3.5 * ring) {
                c.specAmount -= 3.5 * ring;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;

			case 15042:
				c.getItems().addSpecialBar(weapon);
			return true;
			
			case 1377:
			case 11730:
			case 4212:
			if(c.specAmount >= 10 * ring) {
                c.specAmount -= 10 * ring;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;
			
			case 11696: //bandos godsword bgs
			if(c.specAmount >= 6.5 * ring) {
                c.specAmount -= 6.5 * ring;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;

			case 11700:
			if(c.specAmount >= 5 * ring) {
                c.specAmount -= 5 * ring;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;

			case 4587:
			case 859:
			case 861:
			case 11235:
			//case 11700:
			case 9705:
			if(c.specAmount >= 5.5 * ring) {
                c.specAmount -= 5.5 * ring;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;
			
			case 19780:
			if(c.specAmount >= 7.5 * ring) {
                c.specAmount -= 7.5 * ring;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;

			
			default:
			return true; // incase u want to test a weapon
		}
	}
	
	public void resetPlayerAttack() {
		c.usingMagic = false;
		c.npcIndex = 0;
		c.faceUpdate(0);
		c.playerIndex = 0;
		c.getPA().resetFollow();
		//c.sendMessage("Reset attack.");
	}
	
	public void pvpMageDamage(int damage) {
    Client o = (Client) PlayerHandler.players[c.playerIndex];
        int i = c.playerIndex;
		if(damage > 70)
			damage = Misc.random(50) + 20;
	if(o.prayerActive[16]){
	damage/=2;
	}
    if (o.playerLevel[o.playerHitpoints] - damage < 0) { 
        damage = o.playerLevel[o.playerHitpoints];
    }
	if (o.vengOn && damage > 0)
			appendVengeance(i, damage);
    c.getPA().addSkillXP((damage*Config.MAGIC_EXP_RATE), 6); 
    c.getPA().addSkillXP((damage*Config.MAGIC_EXP_RATE/3), 3);
    c.getPA().refreshSkill(3);
    c.getPA().refreshSkill(6);      
    o.handleHitMask(damage);
    o.dealDamage(damage);
    o.damageTaken[c.playerId] += damage;
    c.totalPlayerDamageDealt += damage;
    o.updateRequired = true;
    c.doubleHit = false;
    o.getPA().refreshSkill(3);
}
	
	public int getCombatDifference(int combat1, int combat2) {
		if(combat1 > combat2) {
			return (combat1 - combat2);
		}
		if(combat2 > combat1) {
			return (combat2 - combat1);
		}	
		return 0;
	}
	
	/**
	*Get killer id 
	**/
	
	public int getKillerId(int playerId) {
		int oldDamage = 0;
		int count = 0;
		int killerId = 0;
		for (int i = 1; i < Config.MAX_PLAYERS; i++) {	
			if (Server.playerHandler.players[i] != null) {
				if(Server.playerHandler.players[i].killedBy == playerId) {
					if (Server.playerHandler.players[i].withinDistance(Server.playerHandler.players[playerId])) {
						if(Server.playerHandler.players[i].totalPlayerDamageDealt > oldDamage) {
							oldDamage = Server.playerHandler.players[i].totalPlayerDamageDealt;
							killerId = i;
						}
					}	
					Server.playerHandler.players[i].totalPlayerDamageDealt = 0;
					Server.playerHandler.players[i].killedBy = 0;
				}	
			}
		}				
		return killerId;
	}
		
	
	
	double[] prayerData = {
                1, // Thick Skin.
                1, // Burst of Strength.
                1, // Clarity of Thought.
                1, // Sharp Eye.
                1, // Mystic Will.
                2, // Rock Skin.
                2, // SuperHuman Strength.
                2, // Improved Reflexes.
                0.4, // Rapid restore.
                0.6, // Rapid Heal.
                0.6, // Protect Items.
                1.5, // Hawk eye.
                2, // Mystic Lore.
                4, // Steel Skin.
                4, // Ultimate Strength.
                4, // Incredible Reflexes.
                4, // Protect from Magic.
                4, // Protect from Missiles.
                4, // Protect from Melee.
                4, // Eagle Eye.
                4, // Mystic Might.
                1, // Retribution.
                2, // Redemption.
                6, // Smite.
                8, // Chivalry.
                8, // Piety.
        };
	
	public void handlePrayerDrain() {
		c.usingPrayer = false;
		double toRemove = 0.0;
		for (int j = 0; j < prayerData.length; j++) {
			if (c.prayerActive[j]) {
				if(c.worshippedGod == 1 && c.godReputation >= 100){
					prayerData[16] = 2;
					prayerData[17] = 2;
					prayerData[18] = 2;
				} else {
					prayerData[16] = 4;
					prayerData[17] = 4;
					prayerData[18] = 4;			
				}
			
				toRemove += prayerData[j]/20;
				c.usingPrayer = true;
			}
		}
		if (toRemove > 0) {
			toRemove /= (1 + (0.035 * c.playerBonus[11]));		
		}
		c.prayerPoint -= toRemove;
		if (c.prayerPoint <= 0) {
			c.prayerPoint = 1.0 + c.prayerPoint;
			reducePrayerLevel();
		}
	
	}
	public void reducePrayerLevel() {
		if(c.playerLevel[5] - 1 > 0) {
			if(c.playerLevel[3] > 0)
				c.playerLevel[5] -= 1;
		} else {
			c.sendMessage("You have run out of prayer points!");
			c.playerLevel[5] = 0;
			resetPrayers();
			c.prayerId = -1;	
		}
		c.getPA().refreshSkill(5);
	}
	
	public void resetPrayers() {
		for(int i = 0; i < c.prayerActive.length; i++) {
			c.prayerActive[i] = false;
			c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
		}
		c.headIcon = -1;
		c.getPA().requestUpdates();
	}
	
	/**
	* Wildy and duel info
	**/
	
	public boolean checkReqs() {
		if(Server.playerHandler.players[c.playerIndex] == null) {
			return false;
		}
		if (c.playerIndex == c.playerId)
			return false;
		if (c.inPits && Server.playerHandler.players[c.playerIndex].inPits)
			return true;
		if(Server.playerHandler.players[c.playerIndex].inDuelArena() && c.duelStatus != 5 && !c.usingMagic) {
			if(c.arenas() || c.duelStatus == 5) {
				c.sendMessage("You can't challenge inside the arena!");
				return false;
			}
			c.getTradeAndDuel().requestDuel(c.playerIndex);
			return false;
		}
		if(c.duelStatus == 5 && Server.playerHandler.players[c.playerIndex].duelStatus == 5) {
			if(Server.playerHandler.players[c.playerIndex].duelingWith == c.getId()) {
				return true;
			} else {
				c.sendMessage("This isn't your opponent!");
				return false;
			}
		}
		if(!Server.playerHandler.players[c.playerIndex].inWild() && Server.playerHandler.players[c.playerIndex].safeTimer <= 0 && !Server.playerHandler.players[c.playerIndex].isInHighRiskPK() && !Server.playerHandler.players[c.playerIndex].inFunPk() && !Server.playerHandler.players[c.playerIndex].inStakeArena()) {
			c.sendMessage("That player is not in the wilderness.");
			c.stopMovement();
			c.getCombat().resetPlayerAttack();
			return false;
		}
		if(!c.inWild() && c.safeTimer <= 0 && !c.isInHighRiskPK() && !c.inFunPk() && !c.inStakeArena()) {
			c.sendMessage("You are not in the wilderness.");
			c.stopMovement();
			c.getCombat().resetPlayerAttack();
			return false;
		}
		if(Config.COMBAT_LEVEL_DIFFERENCE) {
			int combatDif1 = c.getCombat().getCombatDifference(c.combatLevel, Server.playerHandler.players[c.playerIndex].combatLevel);
			if(combatDif1 > c.wildLevel || combatDif1 > Server.playerHandler.players[c.playerIndex].wildLevel) {
				c.sendMessage("Your combat level difference is too great to attack that player here.");
				c.stopMovement();
				c.getCombat().resetPlayerAttack();
				return false;
			}
		}
		
		if(Config.SINGLE_AND_MULTI_ZONES) {
			if(!Server.playerHandler.players[c.playerIndex].inMulti()) {	// single combat zones
				if(Server.playerHandler.players[c.playerIndex].underAttackBy != c.playerId  && Server.playerHandler.players[c.playerIndex].underAttackBy != 0) {
					c.sendMessage("That player is already in combat.");
					c.stopMovement();
					c.getCombat().resetPlayerAttack();
					return false;
				}
				if(Server.playerHandler.players[c.playerIndex].playerId != c.underAttackBy && c.underAttackBy != 0 /*|| (c.underAttackBy2 > 0)*/) { // testing if combat is cool this way
					c.sendMessage("You are already in combat.");
					c.stopMovement();
					c.getCombat().resetPlayerAttack();
					return false;
				}
			}
		}
		return true;
	}

	
	public boolean checkMultiBarrageReqs(int i) {
		if(Server.playerHandler.players[i] == null) {
			return false;
		}
		if (i == c.playerId)
			return false;
		if (c.inPits && Server.playerHandler.players[i].inPits)
			return true;
		if(!Server.playerHandler.players[i].inWild()) {
			return false;
		}
		if(Config.COMBAT_LEVEL_DIFFERENCE) {
			int combatDif1 = c.getCombat().getCombatDifference(c.combatLevel, Server.playerHandler.players[i].combatLevel);
			if(combatDif1 > c.wildLevel || combatDif1 > Server.playerHandler.players[i].wildLevel) {
				c.sendMessage("Your combat level difference is too great to attack that player here.");
				return false;
			}
		}
		
		if(Config.SINGLE_AND_MULTI_ZONES) {
			if(!Server.playerHandler.players[i].inMulti()) {	// single combat zones
				if(Server.playerHandler.players[i].underAttackBy != c.playerId  && Server.playerHandler.players[i].underAttackBy != 0) {
					return false;
				}
				if(Server.playerHandler.players[i].playerId != c.underAttackBy && c.underAttackBy != 0) {
					c.sendMessage("You are already in combat.");
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	*Weapon stand, walk, run, etc emotes
	**/
	
	public void getPlayerAnimIndex(String weaponName){
		c.playerStandIndex = 0x328;
		c.playerTurnIndex = 0x337;
		c.playerWalkIndex = 0x333;
		c.playerTurn180Index = 0x334;
		c.playerTurn90CWIndex = 0x335;
		c.playerTurn90CCWIndex = 0x336;
		c.playerRunIndex = 0x338;
		if(weaponName.contains("halberd") || weaponName.contains("guthan") || weaponName.contains("spear") || weaponName.contains("banner") || weaponName.contains("Banner")) {
			c.playerStandIndex = 809;
			c.playerWalkIndex = 1146;
			c.playerRunIndex = 1210;
			return;
		}	
		if(weaponName.contains("dharok") || weaponName.contains("bludgeon")) {
			c.playerStandIndex = 0x811;
			c.playerWalkIndex = 0x67F;
			c.playerRunIndex = 0x680;
			return;
		}	
		if(weaponName.contains("sled")) {
			c.playerStandIndex = 1461;
			c.playerWalkIndex = 1468;
			c.playerRunIndex = 1467;
			return;
		}
		if(weaponName.contains("ahrim")) {
			c.playerStandIndex = 809;
			c.playerWalkIndex = 1146;
			c.playerRunIndex = 1210;
			return;
		}
		if(weaponName.contains("verac")) {
			c.playerStandIndex = 1832;
			c.playerWalkIndex = 1830;
			c.playerRunIndex = 1831;
			return;
		}
		if (weaponName.contains("wand") || weaponName.contains("staff")) {
			c.playerStandIndex = 809;
			c.playerRunIndex = 1210;
			c.playerWalkIndex = 1146;
			return;
		}
		if(weaponName.contains("karil")) {
			c.playerStandIndex = 2074;
			c.playerWalkIndex = 2076;
			c.playerRunIndex = 2077;
			return;
		}
		if(weaponName.contains("cannon")) {
			c.playerStandIndex = 2074;
			c.playerWalkIndex = 2076;
			c.playerRunIndex = 2077;
			return;
		}
		if(weaponName.contains("2h sword") || weaponName.contains("godsword") || weaponName.contains("saradomin sw")) {
			c.playerStandIndex = 4300;
			c.playerWalkIndex = 4306;
			c.playerRunIndex = 4305;
			return;
		}						
		if(weaponName.contains("bow")) {
			c.playerStandIndex = 808;
			c.playerWalkIndex = 819;
			c.playerRunIndex = 824;
			return;
		}
		if(weaponName.contains("anchor"))	{
			c.playerStandIndex = 5869;
			c.playerWalkIndex = 5867;
			c.playerRunIndex = 5868;
			return;
		}
		if(weaponName.contains("carpet")) {
			c.playerStandIndex = 2261;
			c.playerWalkIndex = 2262;
			c.playerRunIndex = 2261;
			return;
		}

	switch(c.playerEquipment[c.playerWeapon]) {
			case 19113:
			case 4151:
			case 15998:
			case 16000:
			case 16002:
			case 16004:
			case 16006:
			case 16008:
			case 16010:
			case 16012:
			case 16014:
			case 16016:
			case 16018:
				case 16020:
				case 16022:
				case 16024:
				case 16026:
			c.playerStandIndex = 1832;
			c.playerWalkIndex = 1660;
			c.playerRunIndex = 1661;
			break;
			case 6528:
			case 15039:
			case 13399:
			case 4153:
			c.playerStandIndex = 1662;
			c.playerWalkIndex = 1663;
			c.playerRunIndex = 1664;
			break;
			case 11694:
			case 11696:
			case 11730:
			case 11698:
			case 11700:
			c.playerStandIndex = 4300;
			c.playerWalkIndex = 4306;
			c.playerRunIndex = 4305;
			break;
			case 15037:
			case 15662:
			case 15042:
			c.playerStandIndex = 809;
			break;
			case 1305:
			c.playerStandIndex = 809;
			break;
		}

	}
	
	/**
	* Weapon emotes
	**/
	
	public int getWepAnim(String weaponName) {
		if(c.playerEquipment[c.playerWeapon] <= 0) {
			switch(c.fightMode) {
				case 0:
				return 422;			
				case 2:
				return 423;			
				case 1:
				return 451;
			}
		}

		if(weaponName.contains("knife") || weaponName.contains("dart") || weaponName.contains("javelin") || weaponName.contains("thrownaxe")){
			return 806;
		}
		if(weaponName.contains("anchor"))	{
			return 5865;
		}
		if(weaponName.contains("dragon axe"))	{
			return 439;
		}
		if(weaponName.contains("halberd")) {
			return 440;
		}
		if(weaponName.startsWith("dragon dagger") || weaponName.startsWith("abyssal dagger")) {
			return 402;
		}	
		if(weaponName.endsWith("dagger")) {
			return 412;
		}		
		if(weaponName.contains("2h sword") || weaponName.contains("godsword") || weaponName.contains("aradomin sword")) {
			return 4307;
		}		
		if(weaponName.contains("sword")) {
			return 451;
		}
		if(weaponName.contains("karil") || weaponName.contains("cannon")) {
			return 2075;
		}
		if(weaponName.contains("bow") && !weaponName.contains("'bow")) {
			return 426;
		}

		if(weaponName.contains("spear")) {
			switch (c.fightMode) {
			case 0:
				return 2080;
			case 1:
				return 2078;
			case 2:
				return 2081;
			case 3:
				return 2082;
			}
		}

		if (weaponName.contains("'bow"))
			return 4230;

		switch(c.playerEquipment[c.playerWeapon]) { // if you don't want to use strings
			case 6522:
			return 2614;
			case 12926:
			return 884;
				
			case 4153: // granite maul
			return 1665;
			case 19780:
			return 451;
			case 4726: // guthan 
			return 2080;
			case 4747: // torag
			return 0x814;
			
			case 4718://dh axe
			case 13045://bludgeon
			switch (c.fightMode) {
			case 0:
				return 2067;
			case 1:
				return 2067;
			case 2:
				return 2067;
			case 3:
				return 2066;
			}
			case 4710: // ahrim
			return 406;
			case 4755: // verac
			return 2062;
			case 4734: // karil
			return 2075;
			case 4151:
			case 16018:
			case 16020:
			case 16022:
			case 16024:
			case 16026:
			case 16000:
			case 16002:
			case 16004:
			case 16006:
			case 16008:
			case 16010:
			case 16012:
			case 16014:
			case 16016:
			case 15998:
			case 19113:
			return 1658;
			case 6528:
			case 15039:
			case 13399:
			return 2661;
			case 13902:
			return 451;
			case 15037:
			case 15662:
			case 15042:
			return 400;
			default:
			return 451;
		}
	}
	
	/**
	* Block emotes
	*/
	public int getBlockEmote() {
		if (c.playerEquipment[c.playerShield] >= 8844 && c.playerEquipment[c.playerShield] <= 8850 || c.playerEquipment[c.playerShield] == 20072) {
			return 4177;
		}
		switch(c.playerEquipment[c.playerWeapon]) {
			case 11716:
				return 410;
			case 4755:
			return 2063;
			
			case 4153:
			return 1666;
			
			case 4151:
			case 19113:
			case 16018:
			case 16020:
			case 16022:
			case 16024:
			case 16026:
			case 16000:
			case 16002:
			case 16004:
			case 16006:
			case 16008:
			case 16010:
			case 16012:
			case 16014:
			case 16016:
			case 15998:
			return 1659;
			
			case 11694:
			case 11698:
			case 10887:
			case 11700:
			case 11696:
			case 11730:
			return -1;
			default:
			return 404;
		}
	}
			
	/**
	* Weapon and magic attack speed!
	**/
	
	public int getAttackDelay(String s) {
		if(s.contains("training bow"))
			return 4;
		if(s.contains("polypore"))
			return 6;
		
		if(s.contains("cannon"))
			return 7;
	
		if(c.usingMagic) {
			switch(c.MAGIC_SPELLS[c.spellId][0]) {
				case 12871: // ice blitz
				case 13023: // shadow barrage
				case 12891: // ice barrage
				return 5;
				
				default:
				return 5;
			}
		}
		if(c.playerEquipment[c.playerWeapon] == -1)
			return 4;//unarmed
			
	switch (c.playerEquipment[c.playerWeapon]) {
			case 12926:
				return 4;
			case 13290: //leaf-blade
				return 4;
			case 11235:
			return 7;
			case 7158:
			return 7;
			case 11730:
			return 4;
			case 15037: //rapiers
			case 15662:
				return 4;
			case 15042:
				return 4;
			case 3757:
				return 4;
			case 19113:
				return 4;
			case 15038:
			case 15574:
			return 5;
			case 18391:
			return 6;
			case 6528:
			return 6;
			case 13045://abyssal bludgeon its whip sheep on osrs. trying it here
			return 4;
			case 15039:
			case 13399:
			return 7;
		}
		
		if(s.endsWith("greataxe"))
			return 7;
		else if(s.contains("anchor"))
			return 7;
		else if(s.equals("torags hammers"))
			return 5;
		else if(s.equals("guthans warspear"))
			return 5;
		else if(s.equals("veracs flail"))
			return 5;
		else if(s.equals("ahrims staff"))
			return 6;
		else if(s.contains("staff")){
			if(s.contains("zamorak") || s.contains("guthix") || s.contains("saradomin") || s.contains("slayer") || s.contains("ancient"))
				return 4;
			else
				return 5;
		} else if(s.contains("bow")){
			if(s.contains("composite") || s.equals("seercull"))
				return 5;
			else if (s.contains("aril"))
				return 4;
			else if(s.contains("Ogre"))
				return 8;
			else if(s.contains("short") || s.contains("hunt") || s.contains("sword") || s.contains("crystal"))
				return 4;
			else if(s.contains("long"))
				return 5;
			else if(s.contains("'bow"))
				return 7;
			
			return 5;
		}
		else if(s.contains("dagger"))
			return 4;
		else if(s.contains("godsword"))
			return 6;
		else if(s.contains("longsword"))
			return 5;
		else if(s.contains("sword"))
			return 4;
		else if(s.contains("scimitar"))
			return 4;
		else if(s.contains("mace"))
			return 5;
		else if(s.contains("battleaxe"))
			return 6;
		else if(s.contains("pickaxe"))
			return 5;
		else if(s.contains("thrownaxe"))
			return 5;
		else if(s.contains("axe"))
			return 5;
		else if(s.contains("warhammer"))
			return 6;
		else if(s.contains("2h"))
			return 7;
		else if(s.contains("spear"))
			return 5;
		else if(s.contains("claw"))
			return 4;
		else if(s.contains("halberd"))
			return 7;
		
		//sara sword, 2400ms
		else if(s.equals("granite maul"))
			return 7;
		else if(s.equals("toktz-xil-ak"))//sword
			return 4;
		else if(s.equals("tzhaar-ket-em"))//mace
			return 5;
		else if(s.equals("tzhaar-ket-om"))//maul
			return 6;
		else if(s.equals("toktz-xil-ek"))//knife
			return 4;
		else if(s.equals("toktz-xil-ul"))//rings
			return 4;
		else if(s.equals("toktz-mej-tal"))//staff
			return 6;
		else if(s.contains("whip"))
			return 4;
		else if(s.contains("dart"))
			return 3;
		else if(s.contains("knife"))
			return 3;
		else if(s.contains("javelin"))
			return 6;
		return 5;
	}
	/**
	* How long it takes to hit your enemy
	**/
	public int getHitDelay(String weaponName) {
		if(c.usingMagic) {
			switch(c.MAGIC_SPELLS[c.spellId][0]) {			
				case 12891:
				return 5;
				case 12871:
				return 5;
				default:
				return 5;
			}
		} else {

			if(weaponName.contains("knife") || weaponName.contains("blowpipe") || weaponName.contains("dart") || weaponName.contains("javelin") || weaponName.contains("thrownaxe")){
				return 3;
			}
			if(weaponName.contains("cross") || weaponName.contains("c'bow")) {
				return 4;
			}
			if(weaponName.contains("bow") && !c.dbowSpec) {
				return 4;
			} else if (c.dbowSpec) {
				return 4;
			}
			
			switch(c.playerEquipment[c.playerWeapon]) {	
				case 6522: // Toktz-xil-ul
				return 3;
				
				default:
				return 2;
			}
		}
	}
	
	public int getRequiredDistance() {
		if (c.followId > 0 && c.freezeTimer <= 0 && !c.isMoving)
			return 2;
		else if(c.followId > 0 && c.freezeTimer <= 0 && c.isMoving) {
			return 3;
		} else {
			return 1;
		}
	}
	
	public boolean usingHally() {
		switch(c.playerEquipment[c.playerWeapon]) {
			case 3190:
			case 3192:
			case 3194:
			case 3196:
			case 3198:
			case 3200:
			case 3202:
			case 3204:
			return true;
			
			default:
			return false;
		}
	}
	
	/**
	* Melee
	**/
	
	public int calculateMeleeAttack() {
		int attackLevel = c.playerLevel[0];
		//2, 5, 11, 18, 19
       if (c.prayerActive[2]) {
            attackLevel += c.getLevelForXP(c.playerXP[c.playerAttack]) * 0.05;
        } else if (c.prayerActive[7]) {
            attackLevel += c.getLevelForXP(c.playerXP[c.playerAttack]) * 0.1;
        } else if (c.prayerActive[15]) {
            attackLevel += c.getLevelForXP(c.playerXP[c.playerAttack]) * 0.15;
        } else if (c.prayerActive[24]) {
            attackLevel += c.getLevelForXP(c.playerXP[c.playerAttack]) * 0.15;
        } else if (c.prayerActive[25]) {
            attackLevel += c.getLevelForXP(c.playerXP[c.playerAttack]) * 0.2;
        }
        if (c.fullVoidMelee())
            attackLevel += c.getLevelForXP(c.playerXP[c.playerAttack]) * 0.1;
		if (c.fullFremmy())
            attackLevel += c.getLevelForXP(c.playerXP[c.playerAttack]) * 0.05;
			//SARA SWORD WAS OP AS FUCK
	if (c.playerEquipment[c.playerWeapon] == 10887) {
		attackLevel += c.getLevelForXP(c.playerXP[c.playerAttack]) * 0.5;
	}
	if (c.playerEquipment[c.playerWeapon] == 11730) {
		attackLevel += c.getLevelForXP(c.playerXP[c.playerAttack]) * 0.1;
	}
	if (c.playerEquipment[c.playerWeapon] == 13290 && c.npcIndex > 0) {
		attackLevel *= 1.5;
	}
	/*if (c.playerEquipment[c.playerWeapon] == 3757) {
		attackLevel += c.getLevelForXP(c.playerXP[c.playerAttack]) * 0.05;
	}*/
		attackLevel *= c.specAccuracy;
		
		/*if(c.playerEquipment[c.playerWeapon] == 4718 && c.playerEquipment[c.playerHat] == 4716 && c.playerEquipment[c.playerChest] == 4720 && c.playerEquipment[c.playerLegs] == 4722) {	
				attackLevel *= 1+((double)(c.getPA().getLevelForXP(c.playerXP[3]) / c.playerLevel[3]) / 10);			
		}*/
		
		//c.sendMessage("Attack: " + (attackLevel + (c.playerBonus[bestMeleeAtk()] * 2)));
        int i = c.playerBonus[bestMeleeAtk()];
		i += c.bonusAttack;
		if (c.playerEquipment[c.playerAmulet] == 11128 && c.playerEquipment[c.playerWeapon] == 6528) {
			i *= 1.30;
		}
		/*if(c.playerEquipment[c.playerWeapon] == 4153)
		{
			i -= 2;
		}*/
		double repBonus = 1;
				
		if(c.worshippedGod == 2 && c.godReputation2 >= 50){
		repBonus = 1.02;
		}
		if(c.worshippedGod == 2 && c.godReputation2 >= 500){
		repBonus = 1.035;
		}
		if(c.worshippedGod == 2 && c.godReputation2 >= 2500){
		repBonus = 1.5;
		}
		return (int)((attackLevel + (attackLevel * 0.15) + (i + i * 0.05)) * repBonus);
	}
	public int bestMeleeAtk()
    {
        if(c.playerBonus[0] > c.playerBonus[1] && c.playerBonus[0] > c.playerBonus[2])
            return 0;
        if(c.playerBonus[1] > c.playerBonus[0] && c.playerBonus[1] > c.playerBonus[2])
            return 1;
        return c.playerBonus[2] <= c.playerBonus[1] || c.playerBonus[2] <= c.playerBonus[0] ? 0 : 2;
    }
	
	public int calculateMeleeMaxHit() {
	
		double maxHit = 0;
		//int strBonus = c.playerBonus[10];
		int strength = c.playerLevel[2];
		int lvlForXP = c.getLevelForXP(c.playerXP[2]);

		if(c.prayerActive[1]) {
			strength += (int)(lvlForXP * .05);
		} else if(c.prayerActive[6]) {
			strength += (int)(lvlForXP * .10);
		} else if(c.prayerActive[14]) {
			strength += (int)(lvlForXP * .15);
		} else if(c.prayerActive[24]) {
			strength += (int)(lvlForXP * .18);
		} else if(c.prayerActive[25]) {
			strength += (int)(lvlForXP * .23);
		}

		maxHit += 1.05D + (double)(strBonus * strength) * 0.00175D;
		maxHit += (double)strength * 0.11D;
		
		if(c.playerEquipment[c.playerHat] == 2526 && c.playerEquipment[c.playerChest] == 2520 && c.playerEquipment[c.playerLegs] == 2522) {	
			maxHit += (maxHit * 10 / 100);
		}
		
		if (c.playerEquipment[c.playerWeapon] == 14484){
			if (maxHit > 43)
				maxHit = 43 + Misc.random(4);
		}

		if(c.playerEquipment[c.playerWeapon] == 4718 && c.playerEquipment[c.playerHat] == 4716 && c.playerEquipment[c.playerChest] == 4720 && c.playerEquipment[c.playerLegs] == 4722) {	
				maxHit *= ((c.getLevelForXP(c.playerXP[3]) - c.playerLevel[3]) * .01) + 1;		
		}
		
		int weaponId = c.playerEquipment[c.playerWeapon];

		if(c.npcIndex > 0 && weaponId == 13290)//increase for leaf blade
			maxHit *= 1.5;

		if(c.playerEquipment[c.playerHat] == 8921) { // black mask
			if(c.npcIndex > 0 && c.onTask(Server.npcHandler.npcs[c.npcIndex].npcType)) {
				maxHit *= 1.075; // if fighting slayer assignment
			} else {
				maxHit *= 1.05; // anything else
			}
		}

		if(c.playerEquipment[c.playerHat] == 13263) { // advanced slayer helm
			if(c.npcIndex > 0 && c.onTask(Server.npcHandler.npcs[c.npcIndex].npcType)) {
				maxHit *= 1.095; // if fighting slayer assignment
			} else {
				maxHit *= 1.07; // anything else
			}
		}

		if(c.playerEquipment[c.playerHat] == 15060) { // slayer helm
			if(c.npcIndex > 0 && c.onTask(Server.npcHandler.npcs[c.npcIndex].npcType)) {
				maxHit *= 1.085; // if fighting slayer assignment
			} else {
				maxHit *= 1.06; // anything else
			}
		}

		if (c.specDamage > 1)
			maxHit = (int)(maxHit * c.specDamage);
		if (maxHit < 0)
			maxHit = 1;
		if (c.fullVoidMelee())
			maxHit = (int)(maxHit * 1.10);
		if (c.fullFremmy())
			maxHit = (int)(maxHit * 1.05);
		if ( c.playerEquipment[c.playerAmulet] == 11128 && (weaponId == 6528 || weaponId == 6523)) {
			maxHit *= 1.20;
		}

		if(c.playerEquipment[c.playerWeapon] == 15574 || c.playerEquipment[c.playerWeapon] == 13399 || c.playerEquipment[c.playerWeapon] == 15662 || c.playerEquipment[c.playerWeapon] == 18391)
			maxHit = (int) (maxHit * c.primalBonus());

		if(c.usingSpecial && c.playerEquipment[c.playerWeapon] == 15042) //Blood rapier spec damage decrease
			maxHit *= 1.10;

		if(c.teleTimer > 0)
			maxHit = 0;
		
		return (int)Math.floor(maxHit);
	}
	

	public int calculateMeleeDefence()
    {
        int defenceLevel = c.playerLevel[1];
		int i = c.playerBonus[bestMeleeDef()];

       if (c.prayerActive[0]) {
            defenceLevel += c.getLevelForXP(c.playerXP[c.playerDefence]) * 0.05;
        } else if (c.prayerActive[5]) {
            defenceLevel += c.getLevelForXP(c.playerXP[c.playerDefence]) * 0.1;
        } else if (c.prayerActive[13]) {
            defenceLevel += c.getLevelForXP(c.playerXP[c.playerDefence]) * 0.15;
        } else if (c.prayerActive[24]) {
            defenceLevel += c.getLevelForXP(c.playerXP[c.playerDefence]) * 0.2;
        } else if (c.prayerActive[25]) {
            defenceLevel += c.getLevelForXP(c.playerXP[c.playerDefence]) * 0.25;
        }
		
		double repBonus = 1;
				
		if(c.worshippedGod == 1 && c.godReputation >= 50){
		repBonus = 1.02;
		}
		if(c.worshippedGod == 1 && c.godReputation >= 500){
		repBonus = 1.035;
		}
		if(c.worshippedGod == 1 && c.godReputation >= 2500){
		repBonus = 1.05;
		}
        return (int)((defenceLevel + (defenceLevel * 0.15) + (i + i * 0.05))*repBonus);
    }
	
	public int bestMeleeDef()
    {
        if(c.playerBonus[5] > c.playerBonus[6] && c.playerBonus[5] > c.playerBonus[7])
            return 5;
        if(c.playerBonus[6] > c.playerBonus[5] && c.playerBonus[6] > c.playerBonus[7])
            return 6;
        return c.playerBonus[7] <= c.playerBonus[5] || c.playerBonus[7] <= c.playerBonus[6] ? 5 : 7;
    }

	/**
	* Range
	**/
	
	public int calculateRangeAttack() {
		int attackLevel = c.playerLevel[4];
		attackLevel *= c.specAccuracy;
        if (c.fullVoidRange())
            attackLevel += c.getLevelForXP(c.playerXP[c.playerRanged]) * 0.1;
		if (c.prayerActive[3])
			attackLevel *= 1.05;
		else if (c.prayerActive[11])
			attackLevel *= 1.10;
		else if (c.prayerActive[19])
			attackLevel *= 1.15;
		//dbow spec
		if (c.playerEquipment[c.playerHat] == 11675 || c.playerEquipment[c.playerHat] == 11664)
			attackLevel *= 1.5;
		if (c.fullVoidRange() && c.specAccuracy > 1.15) {
			attackLevel *= 1.75;		
		}
		
				double repBonus = 1;
				
		if(c.worshippedGod == 2 && c.godReputation2 >= 50){
		repBonus = 1.05;
		}
		if(c.worshippedGod == 2 && c.godReputation2 >= 500){
		repBonus = 1.1;
		}
		if(c.worshippedGod == 2 && c.godReputation2 >= 2500){
		repBonus = 1.15;
		}
        return (int)((attackLevel + (c.playerBonus[4] * 1.95))*repBonus);
	}
	
	public int calculateRangeDefence() {
		int defenceLevel = c.playerLevel[1];
        if (c.prayerActive[0]) {
            defenceLevel += c.getLevelForXP(c.playerXP[c.playerDefence]) * 0.05;
        } else if (c.prayerActive[5]) {
            defenceLevel += c.getLevelForXP(c.playerXP[c.playerDefence]) * 0.1;
        } else if (c.prayerActive[13]) {
            defenceLevel += c.getLevelForXP(c.playerXP[c.playerDefence]) * 0.15;
        } else if (c.prayerActive[24]) {
            defenceLevel += c.getLevelForXP(c.playerXP[c.playerDefence]) * 0.2;
        } else if (c.prayerActive[25]) {
            defenceLevel += c.getLevelForXP(c.playerXP[c.playerDefence]) * 0.25;
        }
		double repBonus = 1;
				
		if(c.worshippedGod == 1 && c.godReputation >= 50){
		repBonus = 1.05;
		}
		if(c.worshippedGod == 1 && c.godReputation >= 500){
		repBonus = 1.1;
		}
		if(c.worshippedGod == 1 && c.godReputation >= 5000){
		repBonus = 1.15;
		}
        return (int)((defenceLevel + c.playerBonus[9] + (c.playerBonus[9] / 2))*repBonus);
	}
	
	public boolean usingBolts() {
		return c.playerEquipment[c.playerArrows] >= 9130 && c.playerEquipment[c.playerArrows] <= 9145 || c.playerEquipment[c.playerArrows] >= 9230 && c.playerEquipment[c.playerArrows] <= 9245;
	}
	public int rangeMaxHit() {
		  int rangeLevel = c.playerLevel[4];
		 
		  int cbowDamage = c.crystalBowSpecDamage;
		  int crystalBowSpecTimer = c.crystalBowSpecTimer; 
		  int weaponId = c.playerEquipment[c.playerWeapon];
		 
		  double modifier = 1.0;
		  double wtf = c.specDamage;
		  int itemUsed = c.usingBow ? c.lastArrowUsed : c.lastWeaponUsed;
		  if (c.prayerActive[3])
		   modifier += 0.05;
		  else if (c.prayerActive[11])
		   modifier += 0.10;
		  else if (c.prayerActive[19])
		   modifier += 0.20;
		  if (c.fullVoidRange())
		   modifier += 0.10;
		  if(weaponId == 15041)
		   modifier += 0.10;
		   
		  double c = modifier * rangeLevel;
		  int rangeStr = getRangeStr(itemUsed);
		  double max =(c + 8) * (rangeStr + 64) / 640;
		  if (wtf != 1)
		   max *= wtf;
		  if (max < 1)
		   max = 1;
		   
		  if (crystalBowSpecTimer > 0 && weaponId == 4212)
		   max += cbowDamage;
		  
		  return (int)max;
		 }
		 public int getRangeStr(int i) {
		  if(i == 4214 || i == 4215 || i == 4216 || i == 4217 || i == 4218 || i == 4219 || i == 4220 || i == 4221 || i == 4222 || i == 4223){
		   return 70;
		}

		if(c.playerEquipment[c.playerWeapon] == 4212)
			return 80;
		if(c.playerEquipment[c.playerWeapon] == 12926)
			return 100;
	
		
		switch (i) {
			//bronze to rune bolts
			case 877:
				return 10;
			case 9140:
				return 46;
			case 9141:
				return 64;
			case 9142:
			case 9241:
			case 9240:
				return 82;
			case 9143:
			case 9243:
			case 9242:
				return 100;
			case 9144:
				return 115;
			case 9244:
				return 125;
			case 9342:
				return 130;
			case 9245:
				return 130;

			

			//bronze to dragon arrows
			case 882:
			return 7;
			case 884:
			return 10;
			case 886:
			return 16;
			case 888:
			return 22;
			case 890:
			return 31;
			case 892:
			return 46;
			case 4740:
			return 55;
			case 11212:
			return 65;
			//knifesknives
			case 864:
			return 3;
			case 863:
			return 4;
			case 865:
			return 7;
			case 866:
			return 10;
			case 867:
			return 14;
			case 868:
			return 24;
			case 13883:
			return 125;
		}
		
		if(c.playerEquipment[c.playerWeapon] == 13022)
			return 156;
		
		return 0;
	}
	
	/*public int rangeMaxHit() {
        int rangehit = 0;
        rangehit += c.playerLevel[4] / 7.5;
        int weapon = c.lastWeaponUsed;
        int Arrows = c.lastArrowUsed;
        if (weapon == 4223) {//Cbow 1/10
            rangehit = 2;
            rangehit += c.playerLevel[4] / 7;
        } else if (weapon == 4222) {//Cbow 2/10
            rangehit = 3;
            rangehit += c.playerLevel[4] / 7;
        } else if (weapon == 4221) {//Cbow 3/10
            rangehit = 3;
            rangehit += c.playerLevel[4] / 6.5;
        } else if (weapon == 4220) {//Cbow 4/10
            rangehit = 4;
            rangehit += c.playerLevel[4] / 6.5;
        } else if (weapon == 4219) {//Cbow 5/10
            rangehit = 4;
            rangehit += c.playerLevel[4] / 6;
        } else if (weapon == 4218) {//Cbow 6/10
            rangehit = 5;
            rangehit += c.playerLevel[4] / 6;
        } else if (weapon == 4217) {//Cbow 7/10
            rangehit = 5;
            rangehit += c.playerLevel[4] / 5.5;
        } else if (weapon == 4216) {//Cbow 8/10
            rangehit = 6;
            rangehit += c.playerLevel[4] / 5.5;
        } else if (weapon == 4215) {//Cbow 9/10
            rangehit = 6;
            rangehit += c.playerLevel[4] / 5;
        } else if (weapon == 4214) {//Cbow Full
            rangehit = 7;
            rangehit += c.playerLevel[4] / 5;
        } else if (weapon == 6522) {
            rangehit = 5;
            rangehit += c.playerLevel[4] / 6;
        } else if (weapon == 11230) {//dragon darts
            rangehit = 8;
            rangehit += c.playerLevel[4] / 10;
        } else if (weapon == 811 || weapon == 868) {//rune darts
            rangehit = 2;
            rangehit += c.playerLevel[4] / 8.5;
        } else if (weapon == 810 || weapon == 867) {//adamant darts
            rangehit = 2;
            rangehit += c.playerLevel[4] / 9;
        } else if (weapon == 809 || weapon == 866) {//mithril darts
            rangehit = 2;
            rangehit += c.playerLevel[4] / 9.5;
        } else if (weapon == 808 || weapon == 865) {//Steel darts
            rangehit = 2;
            rangehit += c.playerLevel[4] / 10;
        } else if (weapon == 807 || weapon == 863) {//Iron darts
            rangehit = 2;
            rangehit += c.playerLevel[4] / 10.5;
        } else if (weapon == 806 || weapon == 864) {//Bronze darts
            rangehit = 1;
            rangehit += c.playerLevel[4] / 11;
        } else if (Arrows == 4740 && weapon == 4734) {//BoltRacks
			rangehit = 3;
            rangehit += c.playerLevel[4] / 4.5;
        } else if (Arrows == 11212) {//dragon arrows
            rangehit = 4;
            rangehit += c.playerLevel[4] / 5.5;
        } else if (Arrows == 892) {//rune arrows
            rangehit = 3;
            rangehit += c.playerLevel[4] / 6;
        } else if (Arrows == 890) {//adamant arrows
            rangehit = 2;
            rangehit += c.playerLevel[4] / 7;
        } else if (Arrows == 888) {//mithril arrows
            rangehit = 2;
            rangehit += c.playerLevel[4] / 7.5;
        } else if (Arrows == 886) {//steel arrows
            rangehit = 2;
            rangehit += c.playerLevel[4] / 8;
        } else if (Arrows == 884) {//Iron arrows
            rangehit = 2;
            rangehit += c.playerLevel[4] / 9;
        } else if (Arrows == 882) {//Bronze arrows
            rangehit = 1;
            rangehit += c.playerLevel[4] / 9.5;
        } else if (Arrows == 9244) {
			rangehit = 8;
			rangehit += c.playerLevel[4] / 3;
		} else if (Arrows == 9139) {
			rangehit = 12;
			rangehit += c.playerLevel[4] / 4;
		} else if (Arrows == 9140) {
			rangehit = 2;
            rangehit += c.playerLevel[4] / 7;
		} else if (Arrows == 9141) {
			rangehit = 3;
            rangehit += c.playerLevel[4] / 6;
		} else if (Arrows == 9142) {
			rangehit = 4;
            rangehit += c.playerLevel[4] / 6;
		} else if (Arrows == 9143) {
			rangehit = 7;
			rangehit += c.playerLevel[4] / 5;
		} else if (Arrows == 9144) {
			rangehit = 7;
			rangehit += c.playerLevel[4] / 4.5;
		}
        int bonus = 0;
        bonus -= rangehit / 10;
        rangehit += bonus;
        if (c.specDamage != 1)
			rangehit *= c.specDamage;
		if (rangehit == 0)
			rangehit++;
		if (c.fullVoidRange()) {
			rangehit *= 1.10;
		}
		if (c.prayerActive[3])
			rangehit *= 1.05;
		else if (c.prayerActive[11])
			rangehit *= 1.10;
		else if (c.prayerActive[19])
			rangehit *= 1.15;
		return rangehit;
    }*/
	
	public boolean properBolts() {
		return c.playerEquipment[c.playerArrows] >= 9140 && c.playerEquipment[c.playerArrows] <= 9144
				|| c.playerEquipment[c.playerArrows] >= 9240 && c.playerEquipment[c.playerArrows] <= 9245;
	}
	
	public int correctBowAndArrows() {
		if (usingBolts())
			return -1;
		switch(c.playerEquipment[c.playerWeapon]) {
			
			case 839:
			case 841:
			return 882;
			
			case 843:
			case 845:
			return 884;
			
			case 847:
			case 849:
			return 886;
			
			case 851:
			case 853:
			return 888;        
			
			case 855:
			case 857:
			return 890;
			
			case 12424:
			case 859:
			case 861:
			return 892;
			
			case 4734:
			case 4935:
			case 4936:
			case 4937:
			return 4740;
			
			case 11235:
			case 9705:
			return 11212;
		}
		return -1;
	}
	
	public int getRangeStartGFX() {
		switch(c.rangeItemUsed) {
			            
			case 863:
			return 220;
			case 864:
			return 219;
			case 865:
			return 221;
			case 866: // knives
			return 223;
			case 867:
			return 224;
			case 868:
			return 225;
			case 869:
			return 222;
			
			case 806:
			return 232;
			case 807:
			return 233;
			case 808:
			return 234;
			case 809: // darts
			return 235;
			case 810:
			return 236;
			case 811:
			return 237;
			case 11230:
			case 12926:
			return 238;
			
			case 825:
			return 206;
			case 826:
			return 207;
			case 827: // javelin
			return 208;
			case 828:
			return 209;
			case 829:
			return 210;
			case 830:
			return 211;

			case 13883:
			return 42;
			case 800:
			return 42;
			case 801:
			return 43;
			case 802:
			return 44; // axes
			case 803:
			return 45;
			case 804:
			return 46;
			case 805:
			return 48;
								
			case 882:
			return 19;
			
			case 884:
			return 18;
			
			case 886:
			return 20;

			case 888:
			return 21;
			
			case 890:
			return 22;
			
			case 892:
			return 24;
			
			case 11212:
			return 26;
			
			case 4212:
			case 4214:
			case 4215:
			case 4216:
			case 4217:
			case 4218:
			case 4219:
			case 4220:
			case 4221:
			case 4222:
			case 4223:
			if(c.crystalBowSpecTimer == 0)
				return 250;
			else
				return -1;
			
		}
		return -1;
	}
		
	public int getRangeProjectileGFX() {
		if (c.dbowSpec) {
			return 672;
		}
		
		if(c.cannonSpec == 1){
			return 88;
		}
		
		if(c.bowSpecShot > 0) {
			switch(c.rangeItemUsed) {
				default:
				return 249;
			}
		}
		if (c.playerEquipment[c.playerWeapon] == 9185 || c.playerEquipment[c.playerWeapon] == 15041)
			return 27;
		switch(c.rangeItemUsed) {
			
			case 863:
			return 213;
			case 864:
			return 212;
			case 865:
			return 214;
			case 866: // knives
			return 216;
			case 867:
			return 217;
			case 868:
			return 218;	
			case 869:
			return 215;  

			case 806:
			return 226;
			case 807:
			return 227;
			case 808:
			return 228;
			case 809: // darts
			return 229;
			case 810:
			return 230;
			case 811:
			return 231;
			case 11230:
			case 12926:
			return 232;	

			case 825:
			return 200;
			case 826:
			return 201;
			case 827: // javelin
			return 202;
			case 828:
			return 203;
			case 829:
			return 204;
			case 830:
			return 205;	
			
			case 6522: // Toktz-xil-ul
			return 442;

			case 800:
			return 36;
			case 801:
			return 35;
			case 802:
			return 37; // axes
			case 803:
			return 38;
			case 804:
			return 39;
			case 805:
			return 40;

			case 882:
			return 10;
			
			case 884:
			return 9;
			
			case 886:
			return 11;

			case 888:
			return 12;
			
			case 890:
			return 13;
			
			case 892:
			return 15;
			
			case 11212:
			return 17;
			
			case 4740: // bolt rack
			return 27;


			
			case 4212:
			case 4214:
			case 4215:
			case 4216:
			case 4217:
			case 4218:
			case 4219:
			case 4220:
			case 4221:
			case 4222:
			case 4223:
			if(c.crystalBowSpecTimer > 0)
				return 473;
			else
				return 249;
			
			
		}
		return -1;
	}
	
	public int getProjectileSpeed() {
		if (c.dbowSpec)
			return 100;
		if(c.playerEquipment[c.playerWeapon] == 9705)
			return 1000;
		return 70;
	}
	
	public int getProjectileShowDelay() {
		switch(c.playerEquipment[c.playerWeapon]) {
			case 863:
			case 864:
			case 865:
			case 866: // knives
			case 867:
			case 868:
			case 869:
			
			case 806:
			case 807:
			case 808:
			case 809: // darts
			case 810:
			case 811:
			case 11230:
			
			case 825:
			case 826:
			case 827: // javelin
			case 828:
			case 829:
			case 830:
			
			case 800:
			case 801:
			case 802:
			case 803: // axes
			case 804:
			case 805:
			case 13883:
			
			case 4734:
            case 9185:
			case 15041:
			case 4935:
			case 4936:
			case 4937:
			return 15; 

			case 12926:
				return 5;
			
		
			default:
			return 5;
		}
	}
	
	/**
	*MAGIC
	**/
	
	public int mageAtk()
    {
        int attackLevel = c.playerLevel[6];

		if (c.fullVoidMage())
            attackLevel *= 1.3;

        if (c.prayerActive[4])
			attackLevel *= 1.05;
		else if (c.prayerActive[12])
			attackLevel *= 1.10;
		else if (c.prayerActive[20])
			attackLevel *= 1.20;

		double repBonus = 1;
		if(c.worshippedGod == 2 && c.godReputation2 >= 50){
		repBonus = 1.05;
		}
		if(c.worshippedGod == 2 && c.godReputation2 >= 500){
		repBonus = 1.1;
		}
		if(c.worshippedGod == 2 && c.godReputation2 >= 5000){
		repBonus = 1.15;
		}
        return (int) ((double)(attackLevel + (c.playerBonus[3] * 2)) * repBonus);
    }
	public int mageDef()
    {
        int defenceLevel = c.playerLevel[1]/2 + c.playerLevel[6]/2;
        if (c.prayerActive[0]) {
            defenceLevel += defenceLevel * 0.05;
        } else if (c.prayerActive[3]) {
            defenceLevel += defenceLevel * 0.1;
        } else if (c.prayerActive[9]) {
            defenceLevel += defenceLevel * 0.15;
        } else if ((c.prayerActive[18] || c.curseActive[9])) {
            defenceLevel += defenceLevel * 0.2;
        } else if (c.prayerActive[19]) {
            defenceLevel += defenceLevel * 0.25;
        }
		double repBonus = 1;
				
		if(c.worshippedGod == 1 && c.godReputation >= 50){
		repBonus = 1.05;
		}
		if(c.worshippedGod == 1 && c.godReputation >= 500){
		repBonus = 1.1;
		}
		if(c.worshippedGod == 1 && c.godReputation >= 5000){
		repBonus = 1.15;
		}
        return (int) ((double)(defenceLevel + (c.playerBonus[8] * 2)) * repBonus);
    }
	
	public boolean wearingStaff(int runeId) {
		int wep = c.playerEquipment[c.playerWeapon];
		switch (runeId) {
			case 554:
			if (wep == 1387)
				return true;
			break;
			case 555:
			if (wep == 1383)
				return true;
			break;
			case 556:
			if (wep == 1381)
				return true;
			break;
			case 557:
			if (wep == 1385)
				return true;
			break;
		}
		return false;
	}
	
	public boolean comboRune(int runeId) {
		switch (runeId) {
			case 554:
			if(c.getItems().playerHasItem(4699))//lava
				return true;
			break;
		}
		return false;
	}
	
	public boolean checkMagicReqs(int spell) {
		if(c.usingMagic && Config.RUNES_REQUIRED) { // check for runes
			if((!c.getItems().playerHasItem(c.MAGIC_SPELLS[spell][8], c.MAGIC_SPELLS[spell][9]) && !wearingStaff(c.MAGIC_SPELLS[spell][8]) && !comboRune(c.MAGIC_SPELLS[spell][8])) ||
				(!c.getItems().playerHasItem(c.MAGIC_SPELLS[spell][10], c.MAGIC_SPELLS[spell][11]) && !wearingStaff(c.MAGIC_SPELLS[spell][10])) ||
				(!c.getItems().playerHasItem(c.MAGIC_SPELLS[spell][12], c.MAGIC_SPELLS[spell][13]) && !wearingStaff(c.MAGIC_SPELLS[spell][12])) ||
				(!c.getItems().playerHasItem(c.MAGIC_SPELLS[spell][14], c.MAGIC_SPELLS[spell][15]) && !wearingStaff(c.MAGIC_SPELLS[spell][14]))){
			c.sendMessage("You don't have the required runes to cast this spell.");
			return false;
			} 
		}

		if(c.usingMagic && c.playerIndex > 0) {
			if(Server.playerHandler.players[c.playerIndex] != null) {
				for(int r = 0; r < c.REDUCE_SPELLS.length; r++){	// reducing spells, confuse etc
					if(Server.playerHandler.players[c.playerIndex].REDUCE_SPELLS[r] == c.MAGIC_SPELLS[spell][0]) {
						c.reduceSpellId = r;
						if((System.currentTimeMillis() - Server.playerHandler.players[c.playerIndex].reduceSpellDelay[c.reduceSpellId]) > Server.playerHandler.players[c.playerIndex].REDUCE_SPELL_TIME[c.reduceSpellId]) {
							Server.playerHandler.players[c.playerIndex].canUseReducingSpell[c.reduceSpellId] = true;
						} else {
							Server.playerHandler.players[c.playerIndex].canUseReducingSpell[c.reduceSpellId] = false;
						}
						break;
					}			
				}
				if(!Server.playerHandler.players[c.playerIndex].canUseReducingSpell[c.reduceSpellId]) {
					c.sendMessage("That player is currently immune to this spell.");
					c.usingMagic = false;
					c.stopMovement();
					resetPlayerAttack();
					return false;
				}
			}
		}

		int staffRequired = getStaffNeeded();
		if(c.usingMagic && staffRequired > 0 && Config.RUNES_REQUIRED) { // staff required
			if(c.playerEquipment[c.playerWeapon] != staffRequired) {
				c.sendMessage("You need a "+c.getItems().getItemName(staffRequired).toLowerCase()+" to cast this spell.");
				return false;
			}
		}
		
		if(c.usingMagic && Config.MAGIC_LEVEL_REQUIRED) { // check magic level
			if(c.playerLevel[6] < c.MAGIC_SPELLS[spell][1]) {
				c.sendMessage("You need to have a magic level of " +c.MAGIC_SPELLS[spell][1]+" to cast this spell.");
				return false;
			}
		}
		if(c.usingMagic && Config.RUNES_REQUIRED) {
			if(c.MAGIC_SPELLS[spell][8] > 0) { // deleting runes
				if (!wearingStaff(c.MAGIC_SPELLS[spell][8])) {
					c.getItems().deleteItem(c.MAGIC_SPELLS[spell][8], c.getItems().getItemSlot(c.MAGIC_SPELLS[spell][8]), c.MAGIC_SPELLS[spell][9]);
					}
			}
			if(c.MAGIC_SPELLS[spell][10] > 0) {
				if (!wearingStaff(c.MAGIC_SPELLS[spell][10]))
					c.getItems().deleteItem(c.MAGIC_SPELLS[spell][10], c.getItems().getItemSlot(c.MAGIC_SPELLS[spell][10]), c.MAGIC_SPELLS[spell][11]);
			}
			if(c.MAGIC_SPELLS[spell][12] > 0) {
				if (!wearingStaff(c.MAGIC_SPELLS[spell][12]))
					c.getItems().deleteItem(c.MAGIC_SPELLS[spell][12], c.getItems().getItemSlot(c.MAGIC_SPELLS[spell][12]), c.MAGIC_SPELLS[spell][13]);
			}
			if(c.MAGIC_SPELLS[spell][14] > 0) {
				if (!wearingStaff(c.MAGIC_SPELLS[spell][14]))
					c.getItems().deleteItem(c.MAGIC_SPELLS[spell][14], c.getItems().getItemSlot(c.MAGIC_SPELLS[spell][14]), c.MAGIC_SPELLS[spell][15]);
			}
		}
		return true;
	}
	
	
	public int getFreezeTime() {
		switch(c.MAGIC_SPELLS[c.oldSpellId][0]) {
			case 1572:
			case 12861: // ice rush
			return 10;
						
			case 1582:
			case 12881: // ice burst
			return 17;
			
			case 1191:
				return 10;
			
			case 1592:
			case 12871: // ice blitz
			return 25;
			
			case 12891: // ice barrage
			return 33;
			
			default:
			return 0;
		}
	}
	
	public void freezePlayer(int i) {
		
	
	}
	
	public void handleGamesNeck() {
		/*if (c.playerIndex > 0 && Server.playerHandler.players[c.playerIndex] != null) {
			int teleport = 15;
			Server.playerHandler.players[c.playerIndex].playerLevel[3] -= teleport;				
		} else {
				c.sendMessage("@red@I shouldn't use this in combat!");
		}*/
	}
	
	
	public void handleDuelRing() {
		/*if (c.playerIndex > 0 && Server.playerHandler.players[c.playerIndex] != null) {
			int teleport = 99;
			Server.playerHandler.players[c.playerIndex].playerLevel[3] -= teleport;				
		} else {
				c.sendMessage("@red@I shouldn't use this in combat!");
		}*/
	}

	public int getStartHeight() {
		switch(c.MAGIC_SPELLS[c.spellId][0]) {
			case 1562: // stun
			return 25;
			
			case 12939:// smoke rush
			return 35;
			
			case 12987: // shadow rush
			return 38;
			
			case 12861: // ice rush
			return 15;
			
			case 12951:  // smoke blitz
			return 38;
			
			case 12999: // shadow blitz
			return 25;
			
			case 12911: // blood blitz
			return 25;
			
			default:
			return 43;
		}
	}
	

	
	public int getEndHeight() {
		switch(c.MAGIC_SPELLS[c.spellId][0]) {
			case 1562: // stun
			return 10;
			
			case 12939: // smoke rush
			return 20;
			
			case 12987: // shadow rush
			return 28;
			
			case 12861: // ice rush
			return 10;
			
			case 12951:  // smoke blitz
			return 28;
			
			case 12999: // shadow blitz
			return 15;
			
			case 12911: // blood blitz
			return 10;
				
			default:
			return 31;
		}
	}
	
	public int getStartDelay() {
		switch(c.MAGIC_SPELLS[c.spellId][0]) {
			case 1539:
			return 60;
			
			case 1190:
			case 1191:
				return 68;
			case 1192:
			return 75;
			
			
			default:
			return 53;
		}
	}
	
	public int getStaffNeeded() {
		switch(c.MAGIC_SPELLS[c.spellId][0]) {
		
			case 1539:
			return 1409;
			
			case 12037:
			return 4170;
			
			case 1190:
			return 2415;
			
			case 1191:
			return 2416;
			
			case 1192:
			return 2417;
			
			default:
			return 0;
		}
	}
	
	public boolean godSpells() {
		switch(c.MAGIC_SPELLS[c.spellId][0]) {	
			case 1190:
			return true;
			
			case 1191:
			return true;
			
			case 1192:
			return true;
			
			default:
			return false;
		}
	}
		
	public int getEndGfxHeight() {
		switch(c.MAGIC_SPELLS[c.oldSpellId][0]) {
			case 12987:	
			case 12901:		
			case 12861:
			case 12445:
			case 1192:
			case 13011:
			case 12919:
			case 12881:
			case 12999:
			case 12911:
			case 12871:
			case 13023:
			case 12929:
			case 12891:
			return 0;
			
			default:
			return 100;
		}
	}
	
	public int getStartGfxHeight() {
		switch(c.MAGIC_SPELLS[c.spellId][0]) {
			case 12871:
			case 12891:
			return 0;
			
			default:
			return 100;
		}
	}
	
	
	

	/*public void handleZerker() {

		if (System.currentTimeMillis() - c.dfsDelay > 60000) {
			if (c.playerIndex > 0 && Server.playerHandler.players[c.playerIndex] != null) {
				int damage = Misc.random(10) + 7;
				c.startAnimation(369);
				c.gfx0(369);
				Server.playerHandler.players[c.playerIndex].playerLevel[3] -= damage;
				Server.playerHandler.players[c.playerIndex].hitDiff2 = damage;
				c.forcedText = "Feel the power of the Berserker Ring!";
				Server.playerHandler.players[c.playerIndex].hitUpdateRequired2 = true;
				Server.playerHandler.players[c.playerIndex].updateRequired = true;
				c.dfsDelay = System.currentTimeMillis();						
			} else {
				c.sendMessage("I should be in combat before using this.");
			
		} else {
			c.sendMessage("My ring hasn't finished recharging yet (60 Seconds)");
}
	}
	public void handleWarrior() {
		if(c.isDonator == 1){
		if (System.currentTimeMillis() - c.dfsDelay > 60000) {
			if (c.playerIndex > 0 && Server.playerHandler.players[c.playerIndex] != null) {
				int damage = Misc.random(10) + 7;
				c.startAnimation(369);
				c.gfx0(369);
				Server.playerHandler.players[c.playerIndex].playerLevel[3] -= damage;
				c.forcedText = "Feel the power of the Warrior Ring!";
				Server.playerHandler.players[c.playerIndex].hitDiff2 = damage;
				Server.playerHandler.players[c.playerIndex].hitUpdateRequired2 = true;
				Server.playerHandler.players[c.playerIndex].updateRequired = true;
				c.dfsDelay = System.currentTimeMillis();						
			} else {
				c.sendMessage("I should be in combat before using this.");
			}
		} else {
			c.sendMessage("My ring hasn't finished recharging yet (60 Seconds)");
			}if (c.isDonator == 0)
			c.sendMessage("Only Donators can use the ring's Special attack");	
		}
	}*/
	
	public void handleSeers() {
/*

		c.castingMagic = true;
		if(c.isDonator == 1){
		if (System.currentTimeMillis() - c.dfsDelay > 60000) {
			if (c.playerIndex > 0 && Server.playerHandler.players[c.playerIndex] != null) {
				int damage = Misc.random(10) + 7;
								c.startAnimation(1979);
								Server.playerHandler.players[c.playerIndex].gfx0(369);
								c.gfx0(368);
					Server.playerHandler.players[c.playerIndex].freezeTimer = 15;
										Server.playerHandler.players[c.playerIndex].resetWalkingQueue();
										Server.playerHandler.players[c.playerIndex].frozenBy = c.playerId;
				Server.playerHandler.players[c.playerIndex].playerLevel[3] -= damage;
				c.forcedText = ("Feel the power of the Seers Ring!");
				Server.playerHandler.players[c.playerIndex].hitDiff2 = damage;

				Server.playerHandler.players[c.playerIndex].hitUpdateRequired2 = true;
				Server.playerHandler.players[c.playerIndex].updateRequired = true;
				c.dfsDelay = System.currentTimeMillis();						
			} else {
				c.sendMessage("I should be in combat before using this.");
			}
		} else {
			c.sendMessage("My ring hasn't finished recharging yet (60 Seconds)");
			}if (c.isDonator == 0)
*/
			//c.sendMessage("Spec comes back soon");	
		
	}
	
	public void handleArcher() {
		/*if(c.isDonator == 1){
		if (System.currentTimeMillis() - c.dfsDelay > 60000) {
			if (c.playerIndex > 0 && Server.playerHandler.players[c.playerIndex] != null) {
				int damage = Misc.random(10) + 7;
				c.startAnimation(369);
				c.gfx0(369);
				Server.playerHandler.players[c.playerIndex].playerLevel[3] -= damage;
				Server.playerHandler.players[c.playerIndex].hitDiff2 = damage;
				c.forcedText = "Feel the power of the Archer Ring!";
				Server.playerHandler.players[c.playerIndex].hitUpdateRequired2 = true;
				Server.playerHandler.players[c.playerIndex].updateRequired = true;
				c.dfsDelay = System.currentTimeMillis();						
			} else {
				c.sendMessage("I should be in combat before using this.");
			}
		} else {
			c.sendMessage("My ring hasn't finished recharging yet (60 Seconds)");
			}if (c.isDonator == 0)
			c.sendMessage("Only Donators can use the ring's Special attack");	
		}*/
	}
	
	public void applyRecoil(int damage, int i) {
		if (damage > 0 && Server.playerHandler.players[i].playerEquipment[c.playerRing] == 2550) {
			int recDamage = damage/10 + 1;
			if (!c.getHitUpdateRequired()) {
				c.setHitDiff(recDamage);
				c.setHitUpdateRequired(true);
				c.getPA().refreshSkill(3);				
			} else if (!c.getHitUpdateRequired2()) {
				c.setHitDiff2(recDamage);
				c.setHitUpdateRequired2(true);
				c.getPA().refreshSkill(3);
			}
			c.dealDamage(recDamage);
			c.updateRequired = true;
			c.getPA().refreshSkill(3);
		}	
	}
	
	public int getBonusAttack(int i) {
		switch (Server.npcHandler.npcs[i].npcType) {
			case 2883:
			return Misc.random(50) + 30;
			case 2026:
			case 2027:
			case 2029:
			case 2030:
			return Misc.random(50) + 30;
		}
		return 0;
	}
	
	
	
	public void handleGmaulPlayer() {
		strBonus = c.playerBonus[10];
		calculateMeleeAttack();
		if (c.playerIndex > 0) {
			Client o = (Client)Server.playerHandler.players[c.playerIndex];
			if (c.goodDistance(c.getX(), c.getY(), o.getX(), o.getY(), getRequiredDistance())) {
			if(c.hitDelay <= 0) {
				if (checkReqs()) {
					if (checkSpecAmount(4153)) {	
						boolean hit = Misc.random(calculateMeleeAttack()) > Misc.random(o.getCombat().calculateMeleeDefence());
						int damage = 0;
						c.specDamage = 1.0;
						c.specAccuracy = 2.5;
						int lvlForXP = c.getLevelForXP(c.playerXP[2]);
						if (hit)
							damage = Misc.random(calculateMeleeMaxHit());
							
						if (c.playerEquipment[c.playerHat] == 11665){
							damage *= 10 / 9;
						}
						
						if (o.playerEquipment[5] == 13742) {//Elysian Effect
							if (Misc.random(100) < 75) {
								double damages = damage;
								double damageDeduction = ((double)damages)/((double)4);
								damage -= ((int)Math.round(damageDeduction));
							}
						}
						if (o.playerEquipment[5] == 13740) {//Divine Effect
							double damages2 = damage;
							double prayer = o.playerLevel[5];
							double possibleDamageDeduction = ((double)damages2)/((double)5);//20% of Damage Inflicted
							double actualDamageDeduction;
							if ((prayer * 2) < possibleDamageDeduction) {
							actualDamageDeduction = (prayer * 2);//Partial Effect(Not enough prayer points)
							} else {
							actualDamageDeduction = possibleDamageDeduction;//Full effect
							}
							double prayerDeduction = ((double)actualDamageDeduction)/((double)2);//Half of the damage deducted
							damage -= ((int)Math.round(actualDamageDeduction));
							o.playerLevel[5] = o.playerLevel[5]-((int)Math.round(prayerDeduction));
							o.getPA().refreshSkill(5);
						}
						
						if ((o.prayerActive[18] || o.curseActive[9]) && System.currentTimeMillis() - o.protMeleeDelay > 1500)
							damage *= .6;
							
						damage *= 0.95;
						applyPlayerHit(c.playerIndex, damage);
						c.startAnimation(1667);
						c.gfx100(340);

						o.updateRequired = true;
						o.getPA().requestUpdates();
					}	
				}	
				}
			}			
		} else if(c.npcIndex > 0) {
			int x = Server.npcHandler.npcs[c.npcIndex].absX;
			int y = Server.npcHandler.npcs[c.npcIndex].absY;
			if (c.goodDistance(c.getX(), c.getY(), x, y, 2)) {
			if(c.hitDelay <= 0) {
					if (c.getCombat().checkSpecAmount(4153)) {
						int damage = Misc.random(c.getCombat().calculateMeleeMaxHit());
						if(Server.npcHandler.npcs[c.npcIndex].HP - damage < 0) {
							damage = Server.npcHandler.npcs[c.npcIndex].HP;
						}
						if(Server.npcHandler.npcs[c.npcIndex].HP > 0) {
							Server.npcHandler.npcs[c.npcIndex].HP -= damage;
							Server.npcHandler.npcs[c.npcIndex].handleHitMask(damage);
							c.startAnimation(1667);
							c.gfx100(340);
						}
					}
					}
				}
				}
	}
	
	public boolean armaNpc(int i) {
		switch (Server.npcHandler.npcs[i].npcType) {
			case 6222:
			case 2559:
			case 2560:
				case 6223:
		case 6225:
		case 6227:
			case 2561:
			return true;	
		}
		return false;	
	}
	
}


