package server.model.players;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Future;

import java.net.InetSocketAddress;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import java.util.Calendar;
import java.util.Date;
import java.text.*;
import java.util.Locale;

import server.util.*;
import server.Connection;

import org.apache.mina.common.IoSession;
import server.Config;
import server.Server;
import server.model.items.ItemAssistant;
import server.model.shops.ShopAssistant;
import server.net.HostList;
import server.net.Packet;
import server.net.StaticPacketBuilder;
import server.util.Misc;
import server.util.Stream;
import server.model.players.skills.*;
import server.event.EventManager;
import server.event.Event;
import server.event.EventContainer;
import server.model.minigames.PestControl;
import server.model.minigames.Pandemonium;
import server.model.minigames.BroodooBrothers;
import server.model.minigames.Boxing;
import server.model.items.BookHandler;
import server.model.npcs.*;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class Client extends Player {
private Pins pins = new Pins(this);
private Pandemonium pandemonium = new Pandemonium(this);
private BroodooBrothers broodoo = new BroodooBrothers(this);
public String lastKilled = "";

private TradeLog tradeLog = new TradeLog(this);

public int bookPage = 0;
public int maxPages = 0;
public String bookName = "Book";
public String[][] bookPages; // Each page(String key) must contain 22 pages.

public int hotspotTimer;

public int damageTimer = 0;
public int killsThisMinute;
public int killsThisMinuteTimer;
public int cantGetKills;
public int cantGetKillsTimer;
public int tradeTimer;
public int yellDelay = 0;
public String lastYell = "";
public String customYellTag = "(Tag)";
public String firstIP = "0";
public String lastIP = "0";
public int offerDelay = 3;
public String customLogin = "You can now change your custom login using ::loginmsg [Message]";
public String yellColor = "@bla@";
public String nameColor = "@bla@";
public boolean attackSkill = false;
public boolean strengthSkill = false;
public boolean defenceSkill = false;
public boolean mageSkill = false;
public boolean rangeSkill = false;
public boolean prayerSkill = false;
public boolean healthSkill = false;
public int lastTrade = 0;

public int[][] randomTeleport = {{2961,3858},
	{2969,3889},
	{2951,3910},
	{2978,3950},
	{3043,10307},
	{3041,10342},
	{3068,3937},
	{3204,3852},
	{3256,3871},
	{3262,3895},
	{3306,3895},
	{3299,3872},
	{3222,3803},
	{3262,3786},
	{3149,3699},//green drags
	{3135,3724}
	};
public int[] clawDamage = { 0, 0, 0 };

public boolean onTask(int i) {

	boolean isOnTask = false;

	switch(slayerTask) {
		case 221:
			if(i == 221)
			isOnTask = true;
		break;
		case 3066:
			if(i == 3066)
			isOnTask = true;
		break;
		case 2881:
			if(i == 2881 || i == 2882 || i == 2883)
			isOnTask = true;
		break;
		case 3200:
			if(i == 3200)
			isOnTask = true;
		break;
		case 50:
			if(i == 50)
			isOnTask = true;
		break;
		case 1351:
			if(i == 1351)
			isOnTask = true;
		break;
		case 1648:
			if(i == 1648 || i == 1653)
			isOnTask = true;
		break;
		case 55:
			if(i == 55)
			isOnTask = true;
		break;
		case 1637:
			if(i == 1637)
			isOnTask = true;
		break;
		case 105:
			if(i == 105 || i == 106 || i == 1326)
			isOnTask = true;
		break;
		case 117:
			if(i == 117)
			isOnTask = true;
		break;
		case 1612:
			if(i == 1612)
			isOnTask = true;
		break;
		case 1643:
			if(i == 1643)
			isOnTask = true;
		break;
		case 1624:
			if(i == 1624)
			isOnTask = true;
		break;
		case 54:
			if(i == 54)
			isOnTask = true;
		break;
		case 2455:
			if(i == 2455 || i == 2881 || i == 2882 || i == 2883 || i == 1351)
			isOnTask = true;
		break;

		default:
			isOnTask = false;
		break;
	}

	return isOnTask;

};

public int[] bossTasks = {221,3066,2881,3200,50,1351};
public int[] bossAmounts = {15,20,25,20,15,10};
public String[][] bossLocation = 
					{
					{"The Black knight titan can be found in the","Taverly dungeon, type ::boss to teleport there."},
					{"Zombie champion is North of the Dark warriors","fortress. Type ::pk and to teleport there."},
					{"You can find the Dagannoth kings by typing ::boss","and running through the caverns."},
					{"The Chaos elemental is East of Mage bank.","Type ::pk to teleport there."},
					{"The King black dragon is found in it's lair.","Type ::boss to teleport there."},
					{"The Dagannoth mother is found in it's lair.","Type ::boss to teleport there."},
					};
public int[] slayerTasks = {1648,55,1637,105,117,1612,1643,1624,1610,1613,1615,2783,2455,54,1459,126};
public int[] slayerAmounts = {40,40,40,40,50,40,40,50,50,50,55,35,35,45,45,50};
public int[] slayerReqs = {1,65,52,12,20,15,45,68,75,80,85,90,55,70,55,70};
public int[] combatReqs = {3,40,50,15,15,30,50,70,80,100,110,115,60,90,100,80};

public String[][] slayerLocation = 
					{
					{"Crawling hands are found in the Slayer tower.","You can type ::slayer to teleport there."},
					{"Blue dragons can be found in the Taverly dungeon.","You can type ::train to teleport there."},
					{"Jellies are found in the Slayer tower.","You can type ::slayer to teleport there."},
					{"Bears can be found in the Bear territory. The area","can be reached by typing ::bears."},
					{"Hill giants can be found in the Taverly dungeon.","You can type ::train to teleport there."},
					{"Banshees can be found in the Slayer tower.","You can type ::slayer to teleport there."},
					{"Infernal mages can be found in the Slayer tower.","You can type ::slayer to teleport there."},
					{"Dust devils can be found in the Slayer tower.","You can type ::slayer to teleport there."},
					{"Gargoyles can be found in the Slayer tower.","You can type ::slayer to teleport there."},
					{"Nechryaels can be found in the Slayer tower.","You can type ::slayer to teleport there."},
					{"Abyssal demons can be found in the Slayer tower.","You can type ::slayer to teleport there."},
					{"Dark beasts can be found in the Slayer tower.","You can type ::slayer to teleport there."},
					{"Dagannoths can be found under the Dagannoth king","Teleport. Type ::boss to access the teleport."},
					{"Black dragons can be found in the Taverly dungeon.","Type ::train to teleport there, go through the pipe."},
					{"Apes can be found at the Land of the apes.","Type ::train to teleport there."},
					{"Otherworldly beings are found in the Donator zone.","Type ::dz to teleport there."}
					};

	public void findTask(){
	//getPA().closeAllWindows();
	int totalTries = 0;
		do {
		String taskName;
		int myTaskNow;
			totalTries++;
			if(totalTries >= 50) {
				sendMessage("Please come back at a higher level.");
				getPA().closeAllWindows();
				break;
				}
			if(combatLevel < 15 && gameMode == 2)
				myTaskNow = 0;
				else
				myTaskNow = Misc.random(slayerTasks.length-1);
			if(slayerTasks[myTaskNow] == 126 && memberStatus < 1) {
				continue;
			}
			if(slayerReqs[myTaskNow] > playerLevel[18]) {//if you are not the slayer level required you will not receive the task.
				continue;
			}
			if(((combatReqs[myTaskNow] > combatLevel) && gameMode == 2) && gameMode != 1) {//if you are not the level required you will not receive the task.
				continue;
			}
			if(((int)combatReqs[myTaskNow] * 1.5) < combatLevel) {//if you are 1.5x level required, you will not receive the task. prevents low tasks
				continue;
			}
			if((((int)slayerReqs[myTaskNow] * 1.5) < playerLevel[18]) && combatLevel >= 15) {//if you are 1.5x level required, you will not receive the task. prevents low tasks
				continue;
			}
			if((slayerTask == slayerTasks[myTaskNow]) && combatLevel >= 15) {//if this was your last task, you will not receive the task
				continue;
			}
					myTaskName = Server.npcHandler.getNpcListName(slayerTasks[myTaskNow]).replaceAll("_", " ");
					myTask = myTaskNow;
					slayerTask = slayerTasks[myTaskNow];
					taskAmount = (slayerAmounts[myTaskNow] / 2) + Misc.random(slayerAmounts[myTaskNow] / 2);
					sendMessage("Your new assignment is to slay "+taskAmount+" "+myTaskName+"s.");
					getDH().sendDialogues(19012,70);
					break;
		} while (taskAmount <= 0 && totalTries <= 50);
	}
	
	public String myTaskName = "";
	
	public void findBossTask(){
	String taskName;
	int myTaskNow2 = 0;
		if(combatLevel <= 75)
			myTaskNow2 = 0;
		if(combatLevel > 75) {
			myTaskNow2 = Misc.random(bossTasks.length-2)+1;
			//if(myTaskNow2 == 0)
				//myTaskNow2
			}
			
					myTaskName = Server.npcHandler.getNpcListName(bossTasks[myTaskNow2]).replaceAll("_", " ");
					if(bossTasks[myTaskNow2] == 2881)
						myTaskName = "Dagannoth king";
					myTask = myTaskNow2;
					slayerTask = bossTasks[myTaskNow2];
					taskAmount = (bossAmounts[myTaskNow2] / 2) + Misc.random(bossAmounts[myTaskNow2] / 2);
					sendMessage("Your new assignment is to slay "+taskAmount+" "+myTaskName+"s.");
					getDH().sendDialogues(19012,70);
	}
	
	public String getTaskName() {
					myTaskName = Server.npcHandler.getNpcListName(slayerTask).replaceAll("_", " ");
					if(slayerTask == 2881)
						myTaskName = "Dagannoth king";
					return myTaskName;
	}

//words blocked from yell and chat!-----------------------------------------------------------
public String[] badwords = {"fuck this server", "nigger", "newfag", "new fag"};
//do not block stupid non offensive words or accidental types - ass, damn, etc.

	private static final String[] serverMessage = {"[@red@DYK?@bla@]: Your Slayer level and XP effect your drop rates!","[@red@DYK?@bla@]: The Giant mole is the only monster that drops the Primal longsword!", "[@red@DYK?@bla@]: Trained accounts recieve double PK and Target Points!", "[@red@DYK?@bla@]: If you've ever had a TokHaar-Kal, you can get it back by typing ::reclaim !", "[@red@DYK?@bla@]: Type ::donate to donate!", "[@red@DYK?@bla@]: The desktop client has less lag!", "[@red@DYK?@bla@]: PvP Armour degrades, but not on trained accounts!", "[@red@DYK?@bla@]: Type ::vote to get some amazing items for free!", "[@red@DYK?@bla@]: We have an active forum community! Type ::forums to check it out.", "[@red@DYK?@bla@]: Thieving is not the best way to make money!", "[@red@DYK?@bla@]: You can turn Mining and Smithing into some serious profit!", "[@red@DYK?@bla@]: Killing the Chaos Elemental is the only way to obtain a Korasi's sword.", "[@red@DYK?@bla@]: The Zombie Champion is the only monster that drops the Primal rapier!", "[@red@DYK?@bla@]: You can turn Bones into Bananas!", "[@red@DYK?@bla@]: You steal PK points from your opponent at ::varrock", "[@red@DYK?@bla@]: Killing any monster gives Slayer XP!", "[@red@DYK?@bla@]: The only way to obtain a Dragon platebody is by voting!", "[@red@DYK?@bla@]: The only way to obtain a Perfect ring is by voting!", "[@red@DYK?@bla@]: Super Donators can spawn Rocktails by typing ::food", "[@red@DYK?@bla@]: You can change your magic book in the quest tab!", "[@red@DYK?@bla@]: There are various commands for items! ::food, ::pots, ::runes, ::veng and more!", "[@red@DYK?@bla@]: You can show off your kills by typing ::kdr !", "[@red@DYK?@bla@]: You can see other people's inventories by typing ::checkinventory player", "[@red@DYK?@bla@]: You can go to either ::funpk or ::pits for safe PKing!", "[@red@DYK?@bla@]: You can use the old HP bar! ::oldbar ::oldhp & ::newbar ::newhp", "[@red@DYK?@bla@]: We have detailed PKing highscores on the website!", "[@red@DYK?@bla@]: You can type ::commands to see a list of commands.", "[@red@DYK?@bla@]: ForeverPkers has been around since 2009!", "[@red@DYK?@bla@]: You can report bugs on the forums for rewards!"};
	public static String getServerMessage() {
		return serverMessage[new java.util.Random().nextInt(serverMessage.length)];
	}

	public int exchange;
	public int clawIndex;
	public int clawType = 0;
	public void betMoney() {
	int randomBet = Misc.random(100);
	if(randomBet <= 40) {
	if (getItems().playerHasItem(995,betAmount)) {
	getItems().addItem(995,betAmount);
	getDH().sendDialogues(959, 3001);
	return;
	}
	} else if (randomBet >= 41) {
	if (getItems().playerHasItem(995,betAmount)) {
	getItems().deleteItem(995, getItems().getItemSlot(995), betAmount);
	getDH().sendDialogues(960, 3001);
	}
	return;
	}
	}

	public long totalRisk() {
				long ShopValue = 0, equipmentValue = 0, invValue = 0, totalValue = 0;
				String totalCost = "", totalCost2 = "";

				getItems().resetKeepItems();
					if(!isSkulled) {	// what items to keep
						getItems().keepItem(0, false);
						getItems().keepItem(1, false);	
						getItems().keepItem(2, false);
					}
					if(redSkull != 1) {
						getItems().keepItem(3, false);
					}
				for (int i = 0; i < playerEquipment.length; i++) {
					int removeId = playerEquipment[i];
					if(removeId == itemKeptId[0] || removeId == itemKeptId[1] || removeId == itemKeptId[2] || removeId == itemKeptId[3])
						continue;
					ShopValue = (long)Math.ceil(Math.ceil(getShops().getItemShopValue(playerEquipment[i]) * .4) * playerEquipmentN[i]);
					equipmentValue += ShopValue;
				}
				for (int j = 0; j < playerItems.length; j++) {
					int removeId = playerItems[j]-1;
					if(removeId == itemKeptId[0] || removeId == itemKeptId[1] || removeId == itemKeptId[2] || removeId == itemKeptId[3])
						continue;
					if((removeId >= 14876 && removeId <= 14887) || removeId == 2365)
						ShopValue = (long)Math.ceil(Math.ceil(getShops().getItemShopValue(playerItems[j]-1)) * playerItemsN[j]);
					else
						ShopValue = (long)Math.ceil(Math.ceil(getShops().getItemShopValue(playerItems[j]-1) * .4) * playerItemsN[j]);
						invValue += ShopValue;
					}
					totalValue = invValue + equipmentValue;

					return totalValue;
			}
	
		
	
	    public static int Barrows[] = {4740, 4734,4710,4724,4726,4728,4730,4718,4718,4732,4736,4738,4716,4720,4722,4753,4747,4755,4757,4759,4708,4712,4714,4745,4749,4751};

        public static int randomBarrows()
        {
            return Barrows[(int)(Math.random()*Barrows.length)];
        }
		
		
		public static int newPKPGamble[] = {Server.shopHandler.ShopItems[18][0]-1,Server.shopHandler.ShopItems[18][1]-1,Server.shopHandler.ShopItems[18][2]-1,Server.shopHandler.ShopItems[18][3]-1,Server.shopHandler.ShopItems[18][4]-1,Server.shopHandler.ShopItems[18][5]-1,Server.shopHandler.ShopItems[18][6]-1,Server.shopHandler.ShopItems[18][7]-1,Server.shopHandler.ShopItems[18][8]-1,Server.shopHandler.ShopItems[18][9]-1,Server.shopHandler.ShopItems[18][10]-1,
			Server.shopHandler.ShopItems[18][11]-1,Server.shopHandler.ShopItems[18][12]-1,Server.shopHandler.ShopItems[18][13]-1,Server.shopHandler.ShopItems[18][14]-1,Server.shopHandler.ShopItems[18][15]-1,Server.shopHandler.ShopItems[18][16]-1,Server.shopHandler.ShopItems[18][17]-1,Server.shopHandler.ShopItems[18][18]-1,Server.shopHandler.ShopItems[18][19]-1,Server.shopHandler.ShopItems[18][20]-1,
			Server.shopHandler.ShopItems[18][21]-1,Server.shopHandler.ShopItems[18][22]-1,Server.shopHandler.ShopItems[18][23]-1,Server.shopHandler.ShopItems[18][24]-1,Server.shopHandler.ShopItems[18][25]-1,Server.shopHandler.ShopItems[18][26]-1,Server.shopHandler.ShopItems[18][27]-1,Server.shopHandler.ShopItems[18][28]-1,Server.shopHandler.ShopItems[18][29]-1,Server.shopHandler.ShopItems[18][30]-1,Server.shopHandler.ShopItems[18][31]-1,Server.shopHandler.ShopItems[18][32]-1,Server.shopHandler.ShopItems[18][33]-1,Server.shopHandler.ShopItems[18][34]-1,Server.shopHandler.ShopItems[18][35]-1,Server.shopHandler.ShopItems[18][36]-1 };

        public static int randomNewPKPGamble()
        {
            return newPKPGamble[(int)(Math.random()*newPKPGamble.length)];
        }

    public static int PK[] = {7462, 6731, 6733, 6735, 6737, 10551, 10548, 8850, 11732};

        public static int randomPK()
        {
            return PK[(int)(Math.random()*PK.length)];
        }
        public static int randomDPresent() {
            return DPresent[(int)(Math.random()*DPresent.length)];
        }


        public static int randomDPresent2() {
            return DPresent2[(int)(Math.random()*DPresent2.length)];
        }


        public static int randomDPresent3() {
            return DPresent3[(int)(Math.random()*DPresent3.length)];
        }


        public static int randomDPresent4() {
            return DPresent4[(int)(Math.random()*DPresent4.length)];
        }


        public static int randomDPresent5() {
            return DPresent5[(int)(Math.random()*DPresent5.length)];
        }

		public static int DPresent[] = { 13047,19113,7462,12708,12710,12712 }; // common
        
        public static int DPresent2[] = { 1053, 1055, 1057, 15998 }; // okay

        public static int DPresent3[] = { 1038,1040,1042,1044,1046,1048,19000 }; // decent

        public static int DPresent4[] = { 11724, 11726, 13744, 13738,3748, 3758, 11284 }; // pretty good
       
        public static int DPresent5[] = { 8921,11694,14484,3758,4212,13045 }; // rare

        public static int DPresent6[] = { 13263 }; // mega rare

        public static int DPresent7[] = { 13742 }; // special


        public static int randomDPresent6() {
            return DPresent6[(int)(Math.random()*DPresent6.length)];
        }

        public static int randomDPresent7() {
            return DPresent7[(int)(Math.random()*DPresent7.length)];
        }

		
		public static int votePresent[] = {15018,15019,15020,15220,19114,15126,6733,6733,6731,6731,6735,6735,6737,6737,6585,6585,11128,2572,20072,11730,4151,4151,4153,7461,7461,11732,11732,10551,10828,10828,8850,8850,10926,10926};

		public static int voteAmount[] = {1,1,1,1,1,1,1,2,1,2,1,2,1,2,1,2,1,1,1,1,1,2,1,1,2,1,2,1,1,2,1,2,5,10};

        public static int randomVote()
        {
            return (int)(Math.random()*votePresent.length);
        }
		
		public static int Phats[] = {1038,1040,1042,1044,1046,1048};

        public static int randomPhat()
        {
            return Phats[(int)(Math.random()*Phats.length)];
        }

        public static int PrimalArmor[] = { 15593, 15592, 15590 };

		public static int ShinyChestItems[] = { 13290 };
		
		public static int ShinyChestItems2[] = { 4224,4212,10330,10332,10334,10336,10338,10340,10342,10344,10346,10348,10350,10352,12422,12424, };
		
		public static int MagicChestItems[] = { 19114, 15126, 7462, 15220, 15018, 15019, 15020, 13679};

        public static int randomChest()
        {
            return ShinyChestItems[(int)(Math.random()*ShinyChestItems.length)];
        }
		
		 public static int randomMagicChest()
        {
            return MagicChestItems[(int)(Math.random()*MagicChestItems.length)];
        }
		
		 public static int randomBadChest()
        {
            return ShinyChestItems2[(int)(Math.random()*ShinyChestItems2.length)];
        }

        public static int CasketItems[] = { Server.shopHandler.ShopItems[31][1]-1, Server.shopHandler.ShopItems[31][2]-1, Server.shopHandler.ShopItems[31][3]-1, Server.shopHandler.ShopItems[31][4]-1,
										Server.shopHandler.ShopItems[31][5]-1, Server.shopHandler.ShopItems[31][6]-1, Server.shopHandler.ShopItems[31][7]-1, Server.shopHandler.ShopItems[31][8]-1, Server.shopHandler.ShopItems[31][9]-1, Server.shopHandler.ShopItems[31][10]-1, Server.shopHandler.ShopItems[31][11]-1, Server.shopHandler.ShopItems[31][12]-1, Server.shopHandler.ShopItems[31][13]-1, Server.shopHandler.ShopItems[31][14]-1, Server.shopHandler.ShopItems[31][15]-1, Server.shopHandler.ShopItems[31][16]-1, Server.shopHandler.ShopItems[31][17]-1, Server.shopHandler.ShopItems[31][18]-1, Server.shopHandler.ShopItems[31][19]-1, Server.shopHandler.ShopItems[31][20]-1, Server.shopHandler.ShopItems[31][21]-1, Server.shopHandler.ShopItems[31][22]-1, Server.shopHandler.ShopItems[31][23]-1, Server.shopHandler.ShopItems[31][24]-1, Server.shopHandler.ShopItems[31][25]-1, Server.shopHandler.ShopItems[31][26]-1, Server.shopHandler.ShopItems[31][27]-1,
										Server.shopHandler.ShopItems[31][5]-1, Server.shopHandler.ShopItems[31][6]-1, Server.shopHandler.ShopItems[31][7]-1, Server.shopHandler.ShopItems[31][8]-1, Server.shopHandler.ShopItems[31][9]-1, Server.shopHandler.ShopItems[31][10]-1, Server.shopHandler.ShopItems[31][11]-1, Server.shopHandler.ShopItems[31][12]-1, Server.shopHandler.ShopItems[31][13]-1, Server.shopHandler.ShopItems[31][14]-1, Server.shopHandler.ShopItems[31][15]-1, Server.shopHandler.ShopItems[31][16]-1, Server.shopHandler.ShopItems[31][17]-1, Server.shopHandler.ShopItems[31][18]-1, Server.shopHandler.ShopItems[31][19]-1, Server.shopHandler.ShopItems[31][20]-1, Server.shopHandler.ShopItems[31][21]-1, Server.shopHandler.ShopItems[31][22]-1, Server.shopHandler.ShopItems[31][23]-1, Server.shopHandler.ShopItems[31][24]-1, Server.shopHandler.ShopItems[31][25]-1, Server.shopHandler.ShopItems[31][26]-1, Server.shopHandler.ShopItems[31][27]-1};

        public static int randomCasket()
        {
            return CasketItems[(int)(Math.random()*CasketItems.length)];
        }

        public static int CasketItems2[] = {
        	1547, 85, //keys
        	11788, // voice of doom
        	11235, 11730, //dbow, sara sword
        	7462,11718,11720,11722,11724,11726,11284, //expensive gear
        	4151,4153,15126,15018,15019,15020,15220,20072,10548,10551,8850,//low items
        	4151,4153,15126,15018,15019,15020,15220,20072,10548,10551,8850
        };

         public static int randomCasket2()
        {
            return CasketItems2[(int)(Math.random()*CasketItems2.length)];
        }
		

	    //public static int DonatorItems[] = {13870, 13873, 13876, 11728, 10362, 11694, 14484, 15001, 15000, 15001, 15037, 15038, 15039, 15037, 15038, 15039, 15040,15041, 11694, 14484, 15000, 15001, 13738, 13740, 13742, 13744, 626, 15037, 15038, 15039, 15040, 1050, 10362, 11728, 2415, 2416, 2417};
public static int DonatorItems[] = { 1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1053, 1055, 1057,1949 };
        public static int randomDonator()
        {
            return DonatorItems[(int)(Math.random()*DonatorItems.length)];
        }

    public static int Wepz[] = {11730, 11700, 11696, 11698, 11694, 11235, 15001, 14484, 4153, 4212};

        public static int randomWeapons()
        {
            return Wepz[(int)(Math.random()*Wepz.length)];
        }
		
	public static int Armor2[] = {10551,10548,626,8850};

        public static int randomArmor2()
        {
            return Armor2[(int)(Math.random()*Armor2.length)];
        }
	public static int Armor3[] = {11724,11726,11718,11720,11722,11728,11728,11284};

        public static int randomArmor3()
        {
            return Armor3[(int)(Math.random()*Armor3.length)];
        }
		
	 public static int Accessories[] = {6733,6731,6735,6737,6585,11128,11090};

        public static int randomAccessories()
        {
            return Accessories[(int)(Math.random()*Accessories.length)];
        }
		
	public static int Accessories2[] = {15018,15020,15019,15220,10362,19114,};

        public static int randomAccessories2()
        {
            return Accessories2[(int)(Math.random()*Accessories2.length)];
        }
		
	public static int Wepz2[] = {4151, 4153, 3204, 11730};

        public static int randomWeapons2()
        {
            return Wepz2[(int)(Math.random()*Wepz2.length)];
        }
		
	public static int Wepz3[] = {14484,11730,15001,11710,11712,11714,11702,11704,11706,11708,11235};

        public static int randomWeapons3()
        {
            return Wepz3[(int)(Math.random()*Wepz3.length)];
        }

	public static int GoodWeapons[] = {15037, 15038, 15039, 15040, 14484};
	
		public static int randomGoodWeapons()
		{
	return GoodWeapons[(int)(Math.random()*GoodWeapons.length)];
		}
		
	public static int Gems [] = { 1617,1617,1617,1617,1617,1617,1617, 1631, 1631, 1631, 1631,1623,1623,1623,1623,1623,1623,1623,1623,1623,1623, 1621, 1621, 1621, 1621, 1621, 1621, 1621, 1621, 1621, 1619,  1619,  1619,  1619,  1619,  1619,  1619,  1619, 1617,1617,1617,1617,1617,1617,1617, 1631, 1631, 1631, 1631,1623,1623,1623,1623,1623,1623,1623,1623,1623,1623, 1621, 1621, 1621, 1621, 1621, 1621, 1621, 1621, 1621, 1619,  1619,  1619,  1619,  1619,  1619,  1619,  1619, 1617,1617,1617,1617,1617,1617,1617, 1631, 1631, 1631, 1631, 6571};
	
		public static int randomGem()
		{
	return Gems[(int)(Math.random()*Gems.length)];
		}
		
		public static int Fish[] = {391,391,391,391, 385, 385, 385, 385,7060,7060,7060,7060, 397};
	
		public static int randomFish()
		{
	return Fish[(int)(Math.random()*Fish.length)];
		}

	

    public static int GS[] = {11700, 11696, 11698, 11694};

        public static int randomGodswords()
        {
            return GS[(int)(Math.random()*GS.length)];
        }


    public static int Armor[] = {11724, 11726, 11720, 11722, 11284, 11718};

        public static int randomArmor()
        {
            return Armor[(int)(Math.random()*Armor.length)];
        }

    public static int Voidz[] = {11664, 11663, 11665, 8839, 8840, 8842};

        public static int randomVoid()
        {
            return Voidz[(int)(Math.random()*Voidz.length)];
        }

    public static int Clues[] = {2677, 2678, 2679};

        public static int randomClue()
        {
            return Clues[(int)(Math.random()*Clues.length)];
        }
	public byte buffer[] = null;
	public Stream inStream = null, outStream = null;
	private IoSession session;
	private ItemAssistant itemAssistant = new ItemAssistant(this);
	private ShopAssistant shopAssistant = new ShopAssistant(this);
	private TradeAndDuel tradeAndDuel = new TradeAndDuel(this);
	private Boxing boxing = new Boxing(this);
	private PlayerAssistant playerAssistant = new PlayerAssistant(this);
	//private NPCHandler npcHandler = new NPCHandler();
	private CombatAssistant combatAssistant = new CombatAssistant(this);
	private ActionHandler actionHandler = new ActionHandler(this);
	private PlayerKilling playerKilling = new PlayerKilling(this);
	private DialogueHandler dialogueHandler = new DialogueHandler(this);
	private Queue<Packet> queuedPackets = new LinkedList<Packet>();
	private Potions potions = new Potions(this);
	private PotionMixing potionMixing = new PotionMixing(this);
	private Food food = new Food(this);
	/**
	 * Skill instances
	 */
	private Slayer slayer = new Slayer(this);
	private Runecrafting runecrafting = new Runecrafting(this);
	private Woodcutting woodcutting = new Woodcutting(this);
	private Mining mine = new Mining(this);
	private Agility agility = new Agility(this);
	private Cooking cooking = new Cooking(this);
	private Fishing fish = new Fishing(this);
	private Crafting crafting = new Crafting(this);
	private Smithing smith = new Smithing(this);
	private Prayer prayer = new Prayer(this);
	private Fletching fletching = new Fletching(this);
	private SmithingInterface smithInt = new SmithingInterface(this);
	private Farming farming = new Farming(this);
	private Thieving thieving = new Thieving(this);
	private Firemaking firemaking = new Firemaking(this);
	private Herblore herblore = new Herblore(this);
	private int somejunk;
	public int lowMemoryVersion = 0;
	public int timeOutCounter = 0;		
	public int returnCode = 2; 
	private Future<?> currentTask;
	

	public Client(IoSession s, int _playerId) {
		super(_playerId);
		this.session = s;
		//synchronized(this) {
			outStream = new Stream(new byte[Config.BUFFER_SIZE]);
			outStream.currentOffset = 0;
		//}
		inStream = new Stream(new byte[Config.BUFFER_SIZE]);
		inStream.currentOffset = 0;
		buffer = new byte[Config.BUFFER_SIZE];
	}


public int pieSelect = 0;
public int kebabSelect = 0;
public int chocSelect = 0;
public int bagelSelect = 0;
public int triangleSandwich = 0;
public int squareSandwich = 0;
public int breadSelect = 0;
public int getKillRemaining() {
int killCounter = 20-killCount;
return killCounter;
}
public void updateCount() {
getPA().sendFrame126("NPC killcount: "+killCount+"",16211);
getPA().sendFrame126("Kills required:",16212);
getPA().sendFrame126("Kills remaining: ",16213);
getPA().sendFrame126(" ",16214);
getPA().sendFrame126(" ",16215);
getPA().sendFrame126("20",16216);
getPA().sendFrame126(""+getKillRemaining()+"",16217);
getPA().sendFrame126(" ",16218);
getPA().sendFrame126(" ",16219);
}
public void randomEvent() {
getPA().sendFrame126(" ", 16131);
getPA().showInterface(16135);		
int randomMessage = Misc.random(6);
if(randomMessage == 0) {
getPA().sendFrame126("Please select the pie for a cash reward!", 16145);
pieSelect = 1;
} else if (randomMessage == 1) {
getPA().sendFrame126("Please select the kebab for a cash reward!", 16145);
kebabSelect = 1;
} else if (randomMessage == 2) {
getPA().sendFrame126("Please select the chocolate for a cash reward!", 16145);
chocSelect = 1;
} else if (randomMessage == 3) {
getPA().sendFrame126("Please select the bagel for a cash reward!", 16145);
bagelSelect = 1;
} else if (randomMessage == 4) {
getPA().sendFrame126("Please select the triangle sandwich for a cash reward!", 16145);
triangleSandwich = 1;
} else if (randomMessage == 5) {
getPA().sendFrame126("Please select the square sandwich for a cash reward!", 16145);
squareSandwich = 1;
} else if (randomMessage == 6) {
getPA().sendFrame126("Please select the bread for a cash reward!", 16145);
breadSelect = 1;
}
}
public void HighAndLow(){
			
	if (combatLevel < 15){
			int Low = 3;
			int High = combatLevel + 12;
				getPA().sendFrame126("@gre@"+Low+"@yel@ - "+High+"", 199);
						}
	if (combatLevel > 15 && combatLevel < 114){
			int Low = combatLevel - 12;
			int High = combatLevel + 12;
				getPA().sendFrame126("@gre@"+Low+"@yel@ - "+High+"", 199);
						}
	if (combatLevel > 114){
			int Low = combatLevel - 12;
			int High = 126;
				getPA().sendFrame126("@gre@"+Low+"@yel@ - "+High+"", 199);
						}

						}
		


	public int isFlaggable(){
		//Current flaggable wealth is 100B (Make sure you use 'L' after a number of integer max)   - Ryan
		long flaggableWealth = 100000000000L;
		int flaggableTickets = 500;
		
		if(getItems().wealthCheck() >= flaggableWealth || (getItems().ticketCheck() >= flaggableTickets && (alldp <= getItems().ticketCheck() / 4))){
			return 2;
		}
	
		return 1;
	}

	public void flushOutStream() {	
		if(disconnected || outStream.currentOffset == 0) return;
		//synchronized(this) {	
			StaticPacketBuilder out = new StaticPacketBuilder().setBare(true);
			byte[] temp = new byte[outStream.currentOffset]; 
			System.arraycopy(outStream.buffer, 0, temp, 0, temp.length);
			out.addBytes(temp);
			session.write(out.toPacket());
			outStream.currentOffset = 0;
		//}
    }
	
	public void stillCamera(int x, int y, int height, int speed, int angle) {
		outStream.createFrame(177);
		outStream.writeByte(x / 64);
		outStream.writeByte(y / 64);
		outStream.writeWord(height);
		outStream.writeByte(speed);
		outStream.writeByte(angle);
	}

	public void spinCamera(int x, int y, int height, int speed, int angle) {
		outStream.createFrame(166);
		outStream.writeByte(x);
		outStream.writeByte(y);
		outStream.writeWord(height);
		outStream.writeByte(speed);
		outStream.writeByte(angle);
	}

	public void resetCamera() {
		outStream.createFrame(107);
		updateRequired = true;
		appearanceUpdateRequired = true;
	}	

	public void sendClan(String name, String message, String clan, int rights) {
		outStream.createFrameVarSizeWord(217);
		outStream.writeString(name);
		outStream.writeString(message);
		outStream.writeString(clan);
		outStream.writeWord(rights);
		outStream.endFrameVarSize();
	}
	
	public static final int PACKET_SIZES[] = {
		0, 0, 0, 1, -1, 0, 0, 0, 0, 0, //0
		0, 0, 0, 0, 8, 0, 6, 2, 2, 0,  //10
		0, 2, 0, 6, 0, 12, 0, 0, 0, 0, //20
		0, 0, 0, 0, 0, 8, 4, 0, 0, 2,  //30
		2, 6, 0, 6, 0, -1, 0, 0, 0, 0, //40
		0, 0, 0, 12, 0, 0, 0, 8, 8, 12, //50
		8, 8, 0, 0, 0, 0, 0, 0, 0, 0,  //60
		6, 0, 2, 2, 8, 6, 0, -1, 0, 6, //70
		0, 0, 0, 0, 0, 1, 4, 6, 0, 0,  //80
		0, 0, 0, 0, 0, 3, 0, 0, -1, 0, //90
		0, 13, 0, -1, 0, 0, 0, 0, 0, 0,//100
		0, 0, 0, 0, 0, 0, 0, 6, 0, 0,  //110
		1, 0, 6, 0, 0, 0, -1, 0, 2, 6, //120
		0, 4, 6, 8, 0, 6, 0, 0, 0, 2,  //130
		0, 0, 0, 0, 0, 6, 0, 0, 0, 0,  //140
		0, 0, 1, 2, 0, 2, 6, 0, 0, 0,  //150
		0, 0, 0, 0, -1, -1, 0, 0, 0, 0,//160
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  //170
		0, 8, 0, 3, 0, 2, 0, 0, 8, 1,  //180
		0, 0, 12, 0, 0, 0, 0, 0, 0, 0, //190
		2, 0, 0, 0, 0, 0, 0, 0, 4, 0,  //200
		4, 0, 0, 0, 7, 8, 0, 0, 10, 0, //210
		0, 0, 0, 0, 0, 0, -1, 0, 6, 0, //220
		1, 0, 0, 0, 6, 0, 6, 8, 1, 0,  //230
		0, 4, 0, 0, 0, 0, -1, 0, -1, 4,//240
		0, 0, 6, 6, 0, 0, 0            //250
	};

	public void destruct() {
		//synchronized (this) {
		if(session == null) 
			return;
		if(underAttackBy > 0 || underAttackBy2 > 0)
			return;
		if(inBoxIsland()) {
			getItems().dropAllItems();
			getItems().deleteAllItems();
			getPA().movePlayer(3182,3442,0);
			isSkulled = false;
			//c.headIconPk = -1;
			redSkull = 0;
			getPA().requestUpdates();
			canUsePortal = 0;
			Client o = (Client)Server.playerHandler.players[playerIndex];
				o.canUsePortal = 1;
		}
		if(!inTrade && !inSecondWindow)//dont save in trade
			PlayerSave.saveGame(this);//dat is voor normale logout ja,maar voor unexpected logout meotn we bij destruct zijn denk ik
		getPA().removeFromCW();
		if (inPits)
			Server.fightPits.removePlayerFromPits(playerId);
		if (clanId >= 0)
			Server.clanChat.leaveClan(playerId, clanId);
		Misc.println("[DEREGISTERED]: "+playerName+"");
		HostList.getHostList().remove(session);
		disconnected = true;
		session.close();
		session = null;
		inStream = null;
		outStream = null;
		isActive = false;
		buffer = null;
		super.destruct();
	//}
}
	
	
	public void sendMessage(String s) {
		//synchronized (this) {
			if(getOutStream() != null) {
				outStream.createFrameVarSize(253);
				outStream.writeString(s);
				outStream.endFrameVarSize();
			}
		//}
	}

		public static final int[][] dupedItems = {//raised whip to 1000 because it's only 2m
	{4067, 10000},{2582, 500},{6917, 500},{6925, 500},{4152, 1000},{11695, 25},{11694, 25},{1042, 10},{1043, 10},{1040, 10},{1041, 10},{1038, 10},{1039, 10},{1042, 10},{1044, 10},
	{1045, 10},{1046, 10},{1047, 10},{1048, 10},{1049, 10},{1050, 10},{1051, 10},{1053, 10},{1054, 10},{1055, 10},{1056, 10},{1057, 10},{1058, 10},{626, 50},{627, 50},{628, 10},{629, 10},

	
	};
	
	public void setSidebarInterface(int menuId, int form) {
		//synchronized (this) {
			if(getOutStream() != null) {
				outStream.createFrame(71);
				outStream.writeWord(form);
				outStream.writeByteA(menuId);
			}
		//}
	}	
	public void updateText() {
}
public int saveTimer;
public int saveTimer2 = 0;

public void updateHighscores(){
/*	int totalz = (KC);
	for (int d = 0; d <= 20; d++) {
		if (totalz >= ranks[d]) {
			if (d == 0) {
				playerRank = d+1;
				ranks[d] = totalz;
				rankPpl[d] = playerName;
			} else if (d < 20){
				if (totalz < ranks[d-1]) {
					playerRank = d+1;
					ranks[d] = totalz;
					rankPpl[d] = playerName;
				}
			}else{
				if (totalz < ranks[d-1]) {
					playerRank = 0;
				}
			}
	}
	}*/
	}


		public int checkDP(String playerName) {
                try {
                        String urlString = "http://localhost/checkdp.php?username="+playerName;
                        urlString = urlString.replaceAll(" ", "%20");
                        URL url = new URL(urlString);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String results = reader.readLine();
						String[] results2 = results.split(" ");
                        if(results.length() > 0) {
						int myDP = (Integer.valueOf(results2[0]));
                            return myDP;
                        }
                } catch (MalformedURLException e) {
                        System.out.println("Malformed URL Exception in checkDP(String playerName)");
                } catch (IOException e) {
                        System.out.println("IO Exception in checkDP(String playerName)");
                }
                return 0;
        }

        public int checkExtra(String playerName) {
                try {
                        String urlString = "http://localhost/checkdp.php?username="+playerName;
                        urlString = urlString.replaceAll(" ", "%20");
                        URL url = new URL(urlString);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String results = reader.readLine();
						String[] results2 = results.split(" ");
                        if(results.length() > 0) {
						int myExtra = (Integer.valueOf(results2[1]));
                            return myExtra;
                        }
                } catch (MalformedURLException e) {
                        System.out.println("Malformed URL Exception in checkDP(String playerName)");
                } catch (IOException e) {
                        System.out.println("IO Exception in checkDP(String playerName)");
                }
                return 0;
        }
		
		public void checkVotes(String playerName) {
                try {
                        String urlString = "http://foreverpkers-ps.com/checkvp.php?username="+playerName;
                        urlString = urlString.replaceAll(" ", "%20");
                        URL url = new URL(urlString);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String results = reader.readLine();
						results = results.replaceAll(" ", "");
                        if(results.length() > 0) {
						int myDP = (Integer.valueOf(results));
						int randomGift = Misc.random(15);
						int theItem = randomVote();
								if(myDP - allVP > 0){
								if(getItems().freeSlots() < 2){
								sendMessage("@red@Not enough inventory space to receive vote reward.");
								return;
								}
									if(myDP - allVP < 30){
									sendMessage("@red@Vote on at least 3 sites totalling 30 points to receive a random gift.");
									} else if (myDP - allVP >= 30){
									sendAll("@dre@"+playerName+" @red@received 1x "+getItems().getItemName(theItem)+" from voting!");
									getItems().addItem(theItem,1);
									}
								VP += myDP - allVP;
								allVP = myDP;
								}
                        }
                } catch (MalformedURLException e) {
                        System.out.println("Malformed URL Exception in checkVotes(String playerName)");
                } catch (IOException e) {
                        System.out.println("IO Exception in checkVotes(String playerName)");
                }
        }

		public void checkVer() {
                try {
                        String urlString = "http://localhost/checkver.php?username="+playerName;
                        urlString = urlString.replaceAll(" ", "%20");
                        URL url = new URL(urlString);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String results = reader.readLine();
						results = results.replaceAll(" ", "");
                        if(results.length() > 0) {
						int myDP = (Integer.valueOf(results));
						verified = myDP;
						if(myDP == 1)
						sendMessage("Your account has been verified successfully!");
						else
						sendMessage("Your account has not yet been verified. Visit www.foreverpkers-ps.com to verify.");
                        } else {
						sendMessage("Your account has not yet been verified. Visit www.foreverpkers-ps.com to verify.");
						}
                } catch (MalformedURLException e) {
                        System.out.println("Malformed URL Exception in checkVer(String playerName)");
                } catch (IOException e) {
                        System.out.println("IO Exception in checkVer(String playerName)");
                }
        }

        

		
		 public int checkReward(String playerName) {
                try {
                        String urlString = "http://foreverpkers-ps.com/checkreward1337x.php?username="+playerName;
                        urlString = urlString.replaceAll(" ", "%20");
                        URL url = new URL(urlString);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String results = reader.readLine();
                        if(results.length() > 0) {
						int myDP = (Integer.valueOf(results));
                                return myDP;
                        }
                } catch (MalformedURLException e) {
                        System.out.println("Malformed URL Exception in checkReward(String playerName)");
                } catch (IOException e) {
                        System.out.println("IO Exception in checkReward(String playerName)");
                }
                return 0;
        }
		
		public int changePass(String name,String password) {
                try {
                        String urlString = "http://foreverpkers-ps.com/xchangepass.php?username="+name+"&key="+password+"&joint=wowsad";
                        urlString = urlString.replaceAll(" ", "%20");
                        URL url = new URL(urlString);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String results = reader.readLine();
                } catch (MalformedURLException e) {
                        System.out.println("Malformed URL Exception in changePass");
                } catch (IOException e) {
                        System.out.println("IO Exception in changePass");
                }
                return 0;
        }
		
		public int checkPass(String name,String password) {
                try {
                        String urlString = "http://foreverpkers-ps.com/xcheckpass.php?username="+name+"&key="+password;
                        urlString = urlString.replaceAll(" ", "%20");
                        URL url = new URL(urlString);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String results = reader.readLine();
                        if(results.length() > 0) {
						int myDP = (Integer.valueOf(results));
                                return myDP;
                        }
                } catch (MalformedURLException e) {
                        System.out.println("Malformed URL Exception in checkPass");
                } catch (IOException e) {
                        System.out.println("IO Exception in checkPass");
                }
                return 0;
        }
		
		

	public void targetLocation() {
		String loc = "", ation = "";
				if(myTarget >= 0) {
					Client o = (Client) Server.playerHandler.players[myTarget];
					if(o != null){
						if(o.absY > absY)
							loc += "North";
						if(o.absY < absY)
							loc += "South";
						if(o.absX > absX)
							ation += "East";
						if(o.absX < absX)
							ation += "West";
						if(loc.length() >= 3 && ation.length() >= 3)
							sendMessage("@red@Your target is @dre@"+loc+"-"+ation+"@red@ of your location.");
						else
							sendMessage("@red@Your target is @dre@"+loc+""+ation+"@red@ of your location.");
					}
				} else {
					sendMessage("You don't have a target, wait a while.");
			}
	}

	public void findTarget() {
			if(targetSystem == 1){
				
				double minimum = 25;
				int playerIndex = -100;
				
				for(int i = 0; i < Server.playerHandler.players.length; i++){
					Client potentialTarget = (Client) Server.playerHandler.players[i];	
					boolean samePlayer = false,
							betterKillDifference = false,
							sameTarget = false,
							hasTarget = false,
							isInWild = false,
							isInCombat = false,
							inVarrock = false,
							canFight = false;
					
					if(potentialTarget != null){
					
						if(potentialTarget.inVarrock() || inVarrock())
							inVarrock = true;//bothInVarrock actually means 1 guy is in varrock and 1 guy isn't
							
						if(Math.abs(combatLevel - potentialTarget.combatLevel) <= 5)
							canFight = true;
					
						if(potentialTarget.playerName.equalsIgnoreCase(playerName) || potentialTarget.playerId == playerId)
							samePlayer = true;
							
						if(potentialTarget.target != 0 || target != 0)
							hasTarget = true;
							
						if(potentialTarget.inWild() && inWild() && wildLevel >= 1 && potentialTarget.wildLevel >= 1)
							isInWild = true;
							
						if(potentialTarget.lastTargetName.equalsIgnoreCase(playerName) || lastTargetName.equalsIgnoreCase(potentialTarget.playerName))
							sameTarget = true;
							
						if(killDifference(rating, Server.playerHandler.players[i].rating) < minimum)
							betterKillDifference = true;
								
						if(!samePlayer && betterKillDifference && !sameTarget && !hasTarget && isInWild && !inVarrock && canFight){
							playerIndex = i;
						} else {
						if(minimum <= 500)
						minimum += .25;
							samePlayer = false;
							betterKillDifference = false;
							sameTarget = false;
							hasTarget = false;
							isInWild = false;
							isInCombat = false;
							inVarrock = false;
						}
					}
				}
				
				Client playerSelected;
				
				if(playerIndex != -100){
					playerSelected = (Client) Server.playerHandler.players[playerIndex];	
				} else {
					playerSelected = null;
				}
				
				if(playerSelected != null && playerIndex != -100){
						headIconHints = 2;
						playerSelected.headIconHints = 2;
						getPA().createPlayerHints(10, playerSelected.playerId);
						playerSelected.getPA().createPlayerHints(10, playerId);
						sendMessage("Target found! Your target is "+playerSelected.playerName+". Kills: "+playerSelected.KC+" Deaths: "+playerSelected.DC+" Rating: "+playerSelected.rating);
						playerSelected.sendMessage("Target found! Your target is "+playerName+". Kills: "+KC+" Deaths: "+DC+" Rating:"+rating);
						targetName = playerSelected.playerName;
						playerSelected.targetName = playerName;
						target = 1;
						playerSelected.target = 1;
						myTarget = playerSelected.playerId;
						playerSelected.myTarget = playerId;
						getPA().requestUpdates();
						playerSelected.getPA().requestUpdates();
						failedTargets = 0;
						questTab();
						playerSelected.questTab();
						targetFinderDelay = Misc.random(10);
				} else {
					failedTargets++;
				}
				
		}
	}
	
	
	public double killDifference(double myKills, double otherKills) {
		/*To reduce confusion: I changed the target finding system
		  from comparing kills, to comparing KDR
													-Ryan*/
		if(myKills >= otherKills)
			return myKills - otherKills;
		else
			return otherKills - myKills;
	}

	public void initialize() {
	restoreStatsDelay = System.currentTimeMillis();
	for (int i = 0; i < 2; i++) {
		getPA().writeChatLog("Logged in");
	}
	
	
	
	if (getItems().playerHasItem(20769, 1) && KC < 1000){
			getItems().deleteItem(20769, getItems().getItemSlot(20769), 1);
		}
		if (getItems().playerHasItem(20770, 1) && KC < 1000){
			getItems().deleteItem(20770, getItems().getItemSlot(20770), 1);
		}
		if (playerEquipment[playerCape] == 20769 && KC < 1000){
			getItems().deleteItem(20769, getItems().getItemSlot(20769), 1);
		}
		if (playerEquipment[playerHat] == 20770 && KC < 1000){
			getItems().deleteItem(20770, getItems().getItemSlot(20770), 1);
		}
	
	/*if(warnings > 4) {
		try {	
					String playerToBan = playerName;
					disconnected = true;
					Connection.addNameToBanList(playerToBan);
					Connection.addNameToFile(playerToBan);
					for(int i = 0; i < Config.MAX_PLAYERS; i++) {
						if(Server.playerHandler.players[i] != null) {
							Client o = (Client) Server.playerHandler.players[i];
							o.sendMessage("[@red@Server@bla@] @red@ Server has auto-banned "+playerToBan+" for having 5 warnings.");
						}
					}
		} catch(Exception e) {
					//c.sendMessage("Player Must Be Offline.");
		}
	}*/
	//foodDelay = System.currentTimeMillis();
	//foodDelay = System.nanoTime();
	//getPand().reset();
	isFlagged = isFlaggable();
	if(getPand().inMissionRegion()){
		//getPA().movePlayer(Config.HOME_X + Misc.random(1), Config.HOME_Y + Misc.random(1), 0);
		getPand().startUp(0, currentKC, currentPPoints);
	}
	
	/*if(!inSlayerTower()) {
		if(heightLevel > 0 && isJailed == 0){
			sendMessage("@red@You have been auto-relocated out of height level " + heightLevel);
			getPA().movePlayer(Config.HOME_X + Misc.random(1), Config.HOME_Y + Misc.random(1), 0);
		}
	}*/
	if(isJailed == 1) {
		getPA().movePlayer(2608, 3160, 4);
		sendMessage("You are still in jail. Please wait for a staff member.");
	}
	


//if(altarPrayed == 0) {
setSidebarInterface(5, 5608);
//} else {
//setSidebarInterface(5, 22500);
//}

//updateHighscores();
getPA().sendFrame126("Close Window", 6401);
/*getPA().sendFrame126(" ", 6402);
getPA().sendFrame126(" ", 6403);
getPA().sendFrame126(" ", 6404);

getPA().sendFrame126(" ", 6405);
getPA().sendFrame126("", 640);
getPA().sendFrame126(" ", 6406);
getPA().sendFrame126(" ", 6407);
getPA().sendFrame126(" ", 6408);
getPA().sendFrame126(" ", 6409);
getPA().sendFrame126(" ", 6410);
getPA().sendFrame126(" ", 6411);
getPA().sendFrame126(" ", 8578);
getPA().sendFrame126(" ", 8579);
getPA().sendFrame126(" ", 8580);
getPA().sendFrame126(" ", 8581);
getPA().sendFrame126(" ", 8582);
getPA().sendFrame126(" ", 8583);
getPA().sendFrame126(" ", 8584);
getPA().sendFrame126(" ", 8585);
getPA().sendFrame126(" ", 8586);
getPA().sendFrame126(" ", 8587);
getPA().sendFrame126(" ", 8588);
getPA().sendFrame126(" ", 8589);
getPA().sendFrame126(" ", 8590);
getPA().sendFrame126(" ", 8591);
getPA().sendFrame126(" ", 8592);
getPA().sendFrame126(" ", 8593);
getPA().sendFrame126(" ", 8594);
getPA().sendFrame126(" ", 8595);
getPA().sendFrame126(" ", 8596);
getPA().sendFrame126(" ", 8597);
getPA().sendFrame126(" ", 8598);
getPA().sendFrame126(" ", 8599);
getPA().sendFrame126(" ", 8600);
getPA().sendFrame126(" ", 8601);
getPA().sendFrame126(" ", 8602);
getPA().sendFrame126(" ", 8603);
getPA().sendFrame126(" ", 8604);
getPA().sendFrame126(" ", 8605);
getPA().sendFrame126(" ", 8606);
getPA().sendFrame126(" ", 8607);
getPA().sendFrame126(" ", 8608);
getPA().sendFrame126(" ", 8609);
getPA().sendFrame126(" ", 8610);
getPA().sendFrame126(" ", 8611);
getPA().sendFrame126(" ", 8612);
getPA().sendFrame126(" ", 8613);
getPA().sendFrame126(" ", 8614);
getPA().sendFrame126(" ", 8615);
getPA().sendFrame126(" ", 8616);
getPA().sendFrame126(" ", 8617);*/
		//synchronized (this) {
		/*if(checkPass(playerName,playerPass) == 20){
	saveCharacter = false;
	disconnected = true;
	}*/
			outStream.createFrame(249);		
			outStream.writeByteA(1);		// 1 for members, zero for free
			outStream.writeWordBigEndianA(playerId);
			for (int j = 0; j < Server.playerHandler.players.length; j++) {
				if (j == playerId)
					continue;
				if (Server.playerHandler.players[j] != null) {
					if (Server.playerHandler.players[j].playerName.equalsIgnoreCase(playerName))
						disconnected = true;
				}
			}
			for (int i = 0; i < 25; i++) {
				getPA().setSkillLevel(i, playerLevel[i], playerXP[i]);
				getPA().refreshSkill(i);
			}
			for(int p = 0; p < PRAYER.length; p++) { // reset prayer glows 
				prayerActive[p] = false;
				getPA().sendFrame36(PRAYER_GLOW[p], 0);	
			}
			
			//if (playerName.equalsIgnoreCase("Sanity")) {
				//getPA().sendCrashFrame();
			//}
			getPA().handleWeaponStyle();
			getPA().handleLoginText();
			accountFlagged = getPA().checkForFlags();
			//getPA().sendFrame36(43, fightMode-1);
			getPA().sendFrame36(108, 0);//resets autocast button
			getPA().sendFrame36(172, 1);
			getPA().sendFrame107(); // reset screen
			getPA().setChatOptions(0, 0, 0); // reset private messaging options
			setSidebarInterface(1, 3917);
			setSidebarInterface(2, 638);
			setSidebarInterface(3, 3213);
			setSidebarInterface(4, 1644);
			setSidebarInterface(5, 5608);
			if(playerMagicBook == 0) {
				setSidebarInterface(6, 1151); //modern
			} else if (playerMagicBook == 1) {
				setSidebarInterface(6, 12855); // ancient
			} else if (playerMagicBook == 2) {
				setSidebarInterface(6, 16640); // lunar
			}
			correctCoordinates();
			setSidebarInterface(7, 18128);		
			setSidebarInterface(8, 5065);
			setSidebarInterface(9, 5715);
			setSidebarInterface(10, 2449);
			//setSidebarInterface(11, 4445); // wrench tab
			setSidebarInterface(11, 904); // wrench tab
			setSidebarInterface(12, 147); // run tab
			setSidebarInterface(13, -1);
			setSidebarInterface(0, 2423);
	if(brightness == 1) {
		getPA().sendFrame36(505, 1);
		getPA().sendFrame36(506, 0);
		getPA().sendFrame36(507, 0);
		getPA().sendFrame36(508, 0);
		getPA().sendFrame36(166, 1);
	}
	if(brightness == 2) {
		getPA().sendFrame36(505, 0);
		getPA().sendFrame36(506, 1);
		getPA().sendFrame36(507, 0);
		getPA().sendFrame36(508, 0);
		getPA().sendFrame36(166, 2);
	}
	if(brightness == 3) {
		getPA().sendFrame36(505, 0);
		getPA().sendFrame36(506, 0);
		getPA().sendFrame36(507, 1);
		getPA().sendFrame36(508, 0);
		getPA().sendFrame36(166, 3);
	}
	if(brightness == 4) {
		getPA().sendFrame36(505, 0);
		getPA().sendFrame36(506, 0);
		getPA().sendFrame36(507, 0);
		getPA().sendFrame36(508, 1);
		getPA().sendFrame36(166, 4);
	}

			addStarter();
			questTab();//loads quest tab when you login
			sendMessage("");
			sendMessage("");
			sendMessage("");
			sendMessage("");
			sendMessage("Welcome to ForeverPkers. There are @blu@"+(PlayerHandler.getPlayerCount())+ " @bla@players online.");
			sendMessage("Type @blu@::donate@bla@ to donate, @blu@::vote@bla@ to vote for rewards, and @blu@::forums@bla@ to enter the forums.");
			sendMessage("@red@Need Help? @bla@Type @blu@::commands@bla@ for all server commands.");
			sendMessage("Economy started on @blu@November 30th@bla@");
			sendMessage("@red@Enjoy 10x PK Points, 10x Drop rates, and 10x Experience until the big update!");
			/*if(lastIP != "0")
				sendMessage("@blu@Your account was last logged in with the IP: @red@" + lastIP);*/
			if(lampStart >= System.currentTimeMillis() - 3600000)
			sendMessage("@blu@Bonus XP active for "+(3600000 - (System.currentTimeMillis() - lampStart)) / 60000+" more minutes.");
			if(lampStart2 >= System.currentTimeMillis() - 3600000)
			sendMessage("@blu@Bonus Drop rates active for "+(3600000 - (System.currentTimeMillis() - lampStart2)) / 60000+" more minutes.");
			if(Server.npcHandler.eventType != 0) {
			sendMessage("[@red@Event@bla@] @dre@"+Server.npcHandler.eventName[Server.npcHandler.eventType]+"@red@. "+(2000 - Server.npcHandler.eventTimer) / 100+" "+(Server.npcHandler.eventTimer < 1900 ? "minutes" : "minute")+" remaining!");
				if(eventId != Server.npcHandler.eventId){
					eventId = Server.npcHandler.eventId;
					eventScore = 0;
				}
			}
			//getDH().sendStartInfo("@blu@Merry Christmas!", "@blu@Hope you all enjoy the holidays.", "", "", "@dre@Welcome to ForeverPkers!");
			if(doingStarter == 1)
				sendMessage("[@blu@Notice@bla@] Complete your starter tasks to earn additional starter items!");
			if(redSkull == 1){
				redSkull = 0;
				sendMessage("@red@You are no longer red skulled.");
			}
			
			//if(lastClan.equals(""))
				//lastClan = "help";
		
			safeTimer = 0;
			damageToKril = 0;

			lastBoss = bossesKilled;//sets last boss to bosses killed for pandemonium restart

			if(hasRegistered == 0){
				if(KC > 0 || bestPand > 0){
					register(playerName, playerPass);
				}
			}
			
			/*if(connectedFrom.startsWith("162.253") || connectedFrom.startsWith("162.219") || connectedFrom.startsWith("184.75")) {
				disconnected = true;
			}*/
			/*if(playerName.equalsIgnoreCase("Kode") && !lastIP.equals(firstIP)) {
				disconnected = true;
			}
			if(playerName.equalsIgnoreCase("The Fruit") && !lastIP.equals(firstIP)) {
				disconnected = true;
			}*/

			
			if(gameMode == 0 && KC == 0) {
				//getDH().sendDialogues(18001, npcType);
				maxStats();
				getPA().showInterface(3559);
				canChangeAppearance = true;
				gameMode = 1;
			} else if (gameMode == 0 && KC > 0){
				gameMode = 1;
			}

			Calendar calendar = Calendar.getInstance(Locale.getDefault());
			int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

			if(lastDay != dayOfMonth) {
				bonusPand = 50000;
				lastDay = dayOfMonth;
			}
			
			if(worshippedGod == 2 && godReputation > 0 && godReputation2 == 0){
				godReputation2 = godReputation;
				godReputation = 0;
			}

			int thisScoreReset = 2;
			if(scoreReset < thisScoreReset){
				bestPand = 0;
				scoreReset = thisScoreReset;
			}

		int thisReset = 53; //Number of this reset. Skipped: 5
			
		if(gotShit < thisReset){
			
			bankPin = "";
			setPin = false;

			/*    Points gained after server resets   */

			double membBonus = 1 + (double)(memberStatus * .05); //Member bonus 5-15%

			//pkPoints = (int) (KC * 10 * membBonus); 		//PK Points Kills * 5 * Member bonus (5-15%)
			pkPoints = 0;

			//if(allEP > 0) 								//Event Points 10%
				//EP = (int) (allEP * .1);
			EP = 0;

			//if(totalPandPoints > 0) 					//Pand Points 10%
			//	pandPoints = (int) (totalPandPoints * .1);
			pandPoints = 0;

			//if(totalTargetPoints > 0) 					//Target Points 10%
				//targetPoints = (int) (totalTargetPoints * .1);
			targetPoints = 0;
			
			//VP = (int) (allVP * .1); 					//Vote points 10%
			VP = 0;
			//if(VP > 1000) VP = 1000;

			//taskPoints = (int) (totalTaskPoints * .1); 				//Slayer Task Points 1w5%
			taskPoints = 0;

			/*               End Points              */

			gotShit = thisReset;
			dicer = 0;
			getItems().addItem(841,1);
			getItems().addItem(1323,1);
			getItems().addItem(3025,100);
            getItems().addItem(6686,100);
            getItems().addItem(2443,100);
            getItems().addItem(2437,100);
            getItems().addItem(2441,100);
			getItems().addItem(2445,100);
            getItems().addItem(565,100000);
            getItems().addItem(560,100000);
            getItems().addItem(555,100000);
            getItems().addItem(386,250);
			getItems().addItem(2528,1);
			getItems().addItem(4447,1);
			
			playerMagicBook = 2;
			setSidebarInterface(6, 16640);
			sendMessage("@red@Your magic book has been changed to Lunar. You can change it in the quest tab.");
			SP=1000;
			dp=0;
			alldp=0;
			pkChallenge = 0;
			mageKills = 0;
			rangeKills = 0;
			meleeKills = 0;
			rating = 1300;
			targFights = 0;
			hadTokHaar = 0;
			hadpring = 0;
			/*if(doingStarter == 1 && gameMode != 0) {
				getDH().sendStartInfo("The best way to get to know the server", "is by completing the starter tasks.", "Go to your @red@quest tab@bla@ to get started,", "there are rewards waiting for you!", "@dre@Welcome to ForeverPkers, "+playerName+"");
			}
			if(doingStarter == 0 && gameMode != 0) {
				getDH().sendStartInfo("Teleport to the @or1@shops@bla@ by using @blu@::shops@bla@.", "Talk to the @or1@Legend's Guard@bla@ he will give you", "limited @or1@free items@bla@.", "type @blu@::commands@bla@ for commands.", "@dre@Welcome to ForeverPkers!");
			}*/

			questTab();
		}
		
					
					
					

/*if(inDuelArena()){
getPA().movePlayer(Config.DUELING_RESPAWN_X+(Misc.random(Config.RANDOM_DUELING_RESPAWN)), Config.DUELING_RESPAWN_Y+(Misc.random(Config.RANDOM_DUELING_RESPAWN)), 0);	
}*/
String hostName2 = ((InetSocketAddress) session.getRemoteAddress())
        .getAddress().getHostName();

				if(firstIP == "0"){
				firstIP = connectedFrom;
				}
				lastIP = connectedFrom;
				
				if(firstHost == "0"){
				firstHost = hostName2;
				}
				lastHost = hostName2;
				
				//checkDupe();

				
				/*boolean badWords = false;
				
					for(int i2 = 0; i2 < badwords.length; i2++) {
						if(customLogin.contains(badwords[i2])) {
							sendMessage("@red@Please refrain from using foul language in your Login Message!");
							sendMessage("@red@You will lose your Login Message rights if you pass the filter.");
							badWords = true;
							break;
						}
					}

					for (int j = 0; j < Server.playerHandler.players.length; j++) {//Login messages
						if (Server.playerHandler.players[j] != null) {
							Client c2 = (Client)Server.playerHandler.players[j];
							if(memberStatus > 2 && canLoginMsg == 0){
								if(!customLogin.equals("") && !badWords){
									c2.sendMessage("[@blu@Login Message@bla@] "+customLogin);
								}
							}
						}
					}*/

targetName = "None";

			//if(inVarrockBank()) {
			//getPA().showOption(4, 0,"Box", 3); Removed it for now :\
			//} else {
			getPA().showOption(4, 0,"Trade With", 3);
			//}
			getPA().showOption(5, 0,"Follow", 4);
			splitChat = true;
			getPA().sendFrame36(502, 1);
			getPA().sendFrame36(287, 1);
			getItems().resetItems(3214);
			getItems().sendWeapon(playerEquipment[playerWeapon], getItems().getItemName(playerEquipment[playerWeapon]));
			getItems().resetBonus();
			getItems().getBonus();
			getItems().writeBonus();
			getItems().setEquipment(playerEquipment[playerHat],1,playerHat);
			getItems().setEquipment(playerEquipment[playerCape],1,playerCape);
			getItems().setEquipment(playerEquipment[playerAmulet],1,playerAmulet);
			getItems().setEquipment(playerEquipment[playerArrows],playerEquipmentN[playerArrows],playerArrows);
			getItems().setEquipment(playerEquipment[playerChest],1,playerChest);
			getItems().setEquipment(playerEquipment[playerShield],1,playerShield);
			getItems().setEquipment(playerEquipment[playerLegs],1,playerLegs);
			getItems().setEquipment(playerEquipment[playerHands],1,playerHands);
			getItems().setEquipment(playerEquipment[playerFeet],1,playerFeet);
			getItems().setEquipment(playerEquipment[playerRing],1,playerRing);
			getItems().setEquipment(playerEquipment[playerWeapon],playerEquipmentN[playerWeapon],playerWeapon);
			getCombat().getPlayerAnimIndex(getItems().getItemName(playerEquipment[playerWeapon]).toLowerCase());
			getPA().logIntoPM();
			getItems().addSpecialBar(playerEquipment[playerWeapon]);
			saveTimer = Config.SAVE_TIMER;
			saveCharacter = true;
			Misc.println("[REGISTERED]: "+playerName+"");
			handler.updatePlayer(this, outStream);
			handler.updateNPC(this, outStream);
			flushOutStream();
			getPA().clearClanChat();
			getPA().resetFollow();

			if (autoRet == 1)
				getPA().sendFrame36(172, 1);
			else
				getPA().sendFrame36(172, 0);
			if(!lastClan.equals(""))
				Server.clanChat.handleClanChat(this, lastClan);
			int modY = absY > 6400 ?  absY - 6400 : absY;
			if(!inDag() && !(absX >= 3079 && absX <= 3134 && absY > 9990 && absY <= 9924) && !(absX > 2350 && absX < 2440 && absY > 4700 && absY < 4740) && !(absX > 3207 && absX < 3221 && absY > 3422 && absY < 3437) && !(absX > 2941 && absX < 3060 && absY > 3314 && absY < 3399) && !(absX > 2583 && absX < 2729 && absY > 3255 && absY < 3343)) {
			wildLevel = (((modY - 3520) / 8) + 2);
			}
			if((absX >= 3079 && absX <= 3134 && absY > 9990 && absY <= 9924))
			wildLevel = 5;
			if((absX > 3013 && absX < 3020 && absY > 3630 && absY < 3633) || (absX > 3017 && absX < 3041 && absY > 3620 && absY < 3641))
			wildLevel = 0;
			if(inVarrock())
			wildLevel = 10;
			if(inBlackDLair())
				wildLevel = 52;
			if(inKBD())
				wildLevel = 50;
			if(inDag())
				wildLevel = 56;
			if(absX > 2350 && absX < 2440 && absY > 4690 && absY < 4740)
			wildLevel = 25;
			if(absX > 3013 && absX < 3065 && absY > 10295 && absY < 10360)
			wildLevel = 52;
			
			if(wildLevel < 20) {
				playerLevel[3] = getLevelForXP(playerXP[3]);
				getPA().refreshSkill(3);
				updateRequired = true;
			}
		//}
	}

	public void maxStats() {
		playerXP[0] = getPA().getXPForLevel(99)+5;
		playerLevel[0] = 99;
		getPA().refreshSkill(0);
		playerXP[1] = getPA().getXPForLevel(99)+5;
		playerLevel[1] = 99;
		getPA().refreshSkill(1);
		playerXP[2] = getPA().getXPForLevel(99)+5;
		playerLevel[2] = 99;
		getPA().refreshSkill(2);
		playerXP[3] = getPA().getXPForLevel(99)+5;
		playerLevel[3] = 99;
		getPA().refreshSkill(3);
		playerXP[4] = getPA().getXPForLevel(99)+5;
		playerLevel[4] = 99;
		getPA().refreshSkill(4);
		playerXP[5] = getPA().getXPForLevel(99)+5;
		playerLevel[5] = 99;
		getPA().refreshSkill(5);
		playerXP[6] = getPA().getXPForLevel(99)+5;
		playerLevel[6] = 99;
		getPA().refreshSkill(6);

		getPA().requestUpdates();
		testGuy();
	}
	
	public void testGuy() {
	restoreStatsDelay = System.currentTimeMillis();
	setSidebarInterface(5, 5608);
	getPA().sendFrame126("Close Window", 6401);
	
			outStream.createFrame(249);		
			outStream.writeByteA(1);		// 1 for members, zero for free
			outStream.writeWordBigEndianA(playerId);
			for (int j = 0; j < Server.playerHandler.players.length; j++) {
				if (j == playerId)
					continue;
				if (Server.playerHandler.players[j] != null) {
					if (Server.playerHandler.players[j].playerName.equalsIgnoreCase(playerName))
						disconnected = true;
				}
			}
			for (int i = 0; i < 25; i++) {
				getPA().setSkillLevel(i, playerLevel[i], playerXP[i]);
				getPA().refreshSkill(i);
			}
			for(int p = 0; p < PRAYER.length; p++) { // reset prayer glows 
				prayerActive[p] = false;
				getPA().sendFrame36(PRAYER_GLOW[p], 0);	
			}
			getPA().sendFrame36(108, 0);//resets autocast button
			getPA().sendFrame36(172, 1);
			getPA().sendFrame107(); // reset screen
			setSidebarInterface(1, 3917);
			setSidebarInterface(2, 638);
			setSidebarInterface(3, 3213);
			setSidebarInterface(4, 1644);
			setSidebarInterface(5, 5608);
			if(playerMagicBook == 0) {
				setSidebarInterface(6, 1151); //modern
			} else if (playerMagicBook == 1) {
				setSidebarInterface(6, 12855); // ancient
			} else if (playerMagicBook == 2) {
				setSidebarInterface(6, 16640); // lunar
			}
			setSidebarInterface(7, 18128);		
			setSidebarInterface(8, 5065);
			setSidebarInterface(9, 5715);
			setSidebarInterface(10, 2449);
			//setSidebarInterface(11, 4445); // wrench tab
			setSidebarInterface(11, 904); // wrench tab
			setSidebarInterface(12, 147); // run tab
			setSidebarInterface(13, -1);
			setSidebarInterface(0, 2423);
			getPA().sendFrame36(502, 1);
			getPA().sendFrame36(287, 1);
			getItems().resetItems(3214);
			getItems().sendWeapon(playerEquipment[playerWeapon], getItems().getItemName(playerEquipment[playerWeapon]));
			getItems().resetBonus();
			getItems().getBonus();
			getItems().writeBonus();
			/*handler.updatePlayer(this, outStream);
			handler.updateNPC(this, outStream);
			flushOutStream();*/
	}
	
	public boolean bonusDrops() {
	if(lampStart2 >= System.currentTimeMillis() - 3600000)
		return true;
		
	return false;
	}
	
	public boolean bonusXP() {
	if(lampStart >= System.currentTimeMillis() - 3600000)
		return true;
		
	return false;
	}

	public void update() {
		//synchronized (this) {
			handler.updatePlayer(this, outStream);
			handler.updateNPC(this, outStream);
			flushOutStream();
		//}
	}
	
	public void checkDupe(){
	}

public void setHighscores(){
/*	int totalz = (KC);
	for (int d = 0; d <= 20; d++) {
		if (totalz >= ranks[d]) {
			if (d == 0) {
				playerRank = d+1;
				ranks[d] = totalz;
				rankPpl[d] = playerName;
			} else if (d < 20){
				if (totalz < ranks[d-1]) {
					playerRank = d+1;
					ranks[d] = totalz;
					rankPpl[d] = playerName;
				}
			}else{
				if (totalz < ranks[d-1]) {
					playerRank = 0;
				}
			}
	}
	}*/
	}

	public int countChaosTemple() {
		int playerAmount = 0;
		for (int j = 0; j < Server.playerHandler.players.length; j++) {
			if (Server.playerHandler.players[j] != null) {
			Client c2 = (Client)Server.playerHandler.players[j];
				if(c2.inChaosTemple()) {
					playerAmount++;
					c2.sendMessage("@red@K'ril Tsutsaroth summons minions to help him in battle!");
				}
			}
		}
		return playerAmount;
	}

	public int countChaosTemple2() {
		int playerAmount = 0;
		for (int j = 0; j < Server.playerHandler.players.length; j++) {
			if (Server.playerHandler.players[j] != null) {
			Client c2 = (Client)Server.playerHandler.players[j];
				if(c2.inChaosTemple()) {
					playerAmount++;
				}
			}
		}
		return playerAmount;
	}

	public void sendAll(String message){//sends a message to all players
		for (int j = 0; j < Server.playerHandler.players.length; j++) {
			if (Server.playerHandler.players[j] != null) {
				Client c2 = (Client)Server.playerHandler.players[j];
				c2.sendMessage(""+message);
			}
		}
	}
	
	public void updateStats() {
		int myGodRep = godReputation;
		int myGod = 1;
			if(godReputation > godReputation2){
				myGodRep = godReputation;
				myGod = 1;
			} else if(godReputation2 > godReputation) {
				myGodRep = godReputation2;
				myGod = 2;
				}
				long timeBefore = System.currentTimeMillis();
                try {
                        String urlString = "http://localhost/updateScores.php?username="+playerName+"&kills="+KC+"&deaths="+DC+"&targets="+totalTargetPoints+"&streak="+hStreak+"&tasks="+fightPitsWins+"&god="+myGod+"&rating="+rating+"&slayer="+playerXP[18]+"&godrep="+myGodRep+"&pand="+bestPand+"&pass=dafunck2";
                        urlString = urlString.replaceAll(" ", "%20");
                        URL url = new URL(urlString);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String results = reader.readLine();
                } catch (MalformedURLException e) {
                        System.out.println("Malformed URL Exception in updateStats(String playerName)");
                } catch (IOException e) {
                        System.out.println("IO Exception in updateStats(String playerName)");
                }
                logOther("Highscores: "+(System.currentTimeMillis() - timeBefore));
        }
		
		public void updateAchieves(String username2, int type) {
			long timeBefore = System.currentTimeMillis();
                try {
                        String urlString = "http://localhost/updateAchieves.php?username="+playerName+"&username2="+username2+"&type="+type+"&pass=dafunck2";
                        urlString = urlString.replaceAll(" ", "%20");
                        URL url = new URL(urlString);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String results = reader.readLine();
                } catch (MalformedURLException e) {
                        System.out.println("Malformed URL Exception in updateAchieves(String playerName)");
                } catch (IOException e) {
                        System.out.println("IO Exception in updateAchieves(String playerName)");
                }
                logOther("Achieves: "+(System.currentTimeMillis() - timeBefore));
        }
	
	public void register(String playerName, String playerPass) {
				long timeBefore = System.currentTimeMillis();
                try {
                        String urlString = "http://localhost/xregister.php?username="+playerName+"&pass=dafunck2";
                        urlString = urlString.replaceAll(" ", "%20");
                        URL url = new URL(urlString);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String results = reader.readLine();
						//results = results.replaceAll(" ", "");
                        if(results.length() > 0) {
							  int myDP = (Integer.valueOf(results));
							if(myDP == 0){
							  sendMessage("@red@Your account has already been registered.");
							  hasRegistered = 1;
							  }
							  if(myDP == 1){
									sendMessage("@blu@Registration successful. Your account will now appear on the Highscores.");
									hasRegistered = 1;
							}
                        }
                } catch (MalformedURLException e) {
                        System.out.println("Malformed URL Exception in register(String playerName)");
                } catch (IOException e) {
                        System.out.println("IO Exception in register(String playerName)");
                }
                logOther("Register: "+(System.currentTimeMillis() - timeBefore));
        }

	
	public void logout() {
		//synchronized (this) {
//MYSQL.updatePlayers(PlayerHandler.getPlayerCount());
			if(inTrade || inSecondWindow)
				return;
			if(inNoLogout()) {
				sendMessage("You can't log out during Last Man Standing!");
				return;
			}
			if(isInJail()) {
				sendMessage("You can't log out here!");
				return;
			}
			if(System.currentTimeMillis() - logoutDelay > 10000) {
				if(Misc.random(100) < 2)
					register(playerName, "o");
				//if(hasRegistered == 1)//make sure their account is in mysql before it updates their highscores, else lag
					//updateStats();
				outStream.createFrame(109);
				properLogout = true;
				PlayerSave.saveGame(this);//PlayerSave.saveGame(this);
			} else {
				sendMessage("You must wait a few seconds from being out of combat to logout.");
			}
		//}
	}
	public void SaveGame() {
		//synchronized (this) {
			if(!inTrade && !inSecondWindow)
				PlayerSave.saveGame(this);
			//if(playerRights >= 3)
				//sendMessage("Saved");
		//}
	}
	public void resetRanks() {
		for (int i = 0; i < 20; i++) {
			ranks[i] = 0;
			rankPpl[i] = "";
		}
}

	public void highscores() {
//setHighscores();
		
	//	getPA().sendFrame126("     Top PKers Online", 6399);
	//	for(int i = 0; i < 20; i++) {
	//		if(ranks[i] > 0) {
//if(i<11)
//				getPA().sendFrame126((i+1)+" - "+(rankPpl[i])+ " - Kills: " +ranks[i], 6402+i);
//else
		//		getPA().sendFrame126((i+1)+" - "+(rankPpl[i])+ " - Kills: " +ranks[i], 8578+i-11);
		//	}
		//}
		//getPA().showInterface(6308);
		//flushOutStream();
		//resetRanks();
	}
//end highscores
	public int playerRank = 0;
	public static int[] ranks = new int[21];
    public static String[] rankPpl = new String[21];
	
	public void endTutorial() {
	finishedTutorial = false;
	}
	
public int clawPlayerDelay = 0;
public int clawNPCDelay = 0;
	public int packetSize = 0, packetType = -1;
	public int lastAttacked = 0;
	public long saveGameDelay;
	
	
	
	
	
	
	boolean inHotspot(int hotspotLevel){
		 switch(hotspotLevel){
			  case 1:
			   if(absX >= 3067 && absX <= 3123 && absY >= 3519 && absY <= 3569) //edgeville PK
				return true;
			  break;
			  case 2:
			   if(absX >= 3281 && absX <= 3311 && absY >= 3642 && absY <= 3664)//hills
				return true;
			  break;
			  case 3:
			   if(absX >= 2964 && absX <= 3002 && absY >= 3586 && absY <= 3628)//east
				return true;
			  break;
			  case 4:
			   if(absX >= 3080 && absX <= 3110 && absY >= 3955 && absY <= 3965)//magebank
				return true;
			  break;
		 }
		 return false;
}

public void logSales(String log){
try{
					
					packetLog.open("../cfg/shops");
					packetLog.print(""+log);
					packetLog.close();

					//packetLogString = "null";
				} catch (Exception e){
					System.out.println("Error saving log packets");
				}
}

public void logOther(String log){
try{
					
					packetLog.open("../cfg/laglog");
					packetLog.print(""+log);
					packetLog.close();

					//packetLogString = "null";
				} catch (Exception e){
					System.out.println("Error saving log packets");
				}
}

		public void questTab() {
			getPA().sendFrame126("@or1@  ForeverPkers", 640);
			//getPA().sendFrame126("@or1@FP Points: @gre@"+fpp, 663);
			if(doingStarter == 0) {
				getPA().sendFrame126("@or2@Actions", 663);
				getPA().sendFrame126("@or1@[@cya@Items Kept On Death@or1@]", 7332);
				if(playerMagicBook == 0)
				getPA().sendFrame126("@or1@[@cya@Magic: @gre@Modern@or1@]", 7333);
				if(playerMagicBook == 1)
				getPA().sendFrame126("@or1@[@cya@Magic: @gre@Ancient@or1@]", 7333);
				if(playerMagicBook == 2)
				getPA().sendFrame126("@or1@[@cya@Magic: @gre@Lunar@or1@]", 7333);
				getPA().sendFrame126("@or1@Kills: @gre@"+KC+" @or1@Deaths:@gre@ "+DC, 7334); 
				//getPA().sendFrame126("@or1@Potential: @red@ Removed", 7383);
				getPA().sendFrame126("@or1@Target: @gre@"+targetName, 7383);
				getPA().sendFrame126("@or1@PK Points: @gre@"+pkPoints, 7336);
				getPA().sendFrame126("@or1@Target Points: @gre@"+targetPoints, 7339);
				if(warnings >= 1 && warnings <= 2)  {
					getPA().sendFrame126("@or1@Warnings: @yel@"+warnings, 7338);
				} else if(warnings >= 3) {
					 getPA().sendFrame126("@or1@Warnings: @red@"+warnings, 7338);
				} else {
					getPA().sendFrame126("@or1@Warnings: @gre@"+warnings, 7338);
				}
				getPA().sendFrame126("@or1@Current Killstreak: @gre@"+cStreak, 7340); 
				getPA().sendFrame126("@or1@Highest Killstreak: @gre@"+hStreak, 7346);
				if(worshippedGod == 0)
				getPA().sendFrame126("@or1@God Reputation: @gre@0", 7341);
				if(worshippedGod == 1)
				getPA().sendFrame126("@or1@God Reputation: @gre@"+godReputation, 7341);
				if(worshippedGod == 2)
				getPA().sendFrame126("@or1@God Reputation: @gre@"+godReputation2, 7341);
				getPA().sendFrame126("@or1@Target Rating: @gre@"+rating, 7342);//3433 3463
				getPA().sendFrame126("@gre@Shops (::shop)", 7337);//421, 422
				getPA().sendFrame126("@gre@Bosses (::boss)", 7343);//941
				getPA().sendFrame126("@gre@PK Zones (::PK)", 7335);//5571 npc id: 5321 5568 id: 5312
				getPA().sendFrame126("@gre@Minigames (::MG)", 7344);//anim: 5485 npc: 5337
				getPA().sendFrame126("@gre@Slayer Tower (::slayer)", 7345);
				getPA().sendFrame126("", 7347);
				getPA().sendFrame126("", 7348);
				getPA().sendFrame126("", 12772);
				getPA().sendFrame126("", 673);
				getPA().sendFrame126("", 7352);
				getPA().sendFrame126("", 17510);
				getPA().sendFrame126("", 7353);
				getPA().sendFrame126("", 12129);
				getPA().sendFrame126("", 8438);
				getPA().sendFrame126("", 12852);
				getPA().sendFrame126("", 15841);
			}
			if(doingStarter == 1) {
				getPA().sendFrame126("@or2@Starter Tasks", 663);
				getPA().sendFrame126("@or1@[@cya@Items Kept On Death@or1@]", 7332);
				if(sTask1 == 0)
				getPA().sendFrame126("@or1@Visit ::shops @or2@", 7333);
				if(sTask1 == 1)
				getPA().sendFrame126("@gre@Vist ::shops", 7333);
				if(sTask2 == 0)
				getPA().sendFrame126("@or1@Steal from a stall @or2@", 7334);
				if(sTask2 == 1)
				getPA().sendFrame126("@gre@Steal from a stall", 7334);
				if(sTask3 == 0 && gameMode == 1) {
				getPA().sendFrame126("@or1@Change your attack lvl @or2@", 7383);
				}
				if(sTask3 == 1 && gameMode == 1) {
				getPA().sendFrame126("@gre@Change your attack level", 7383);
				}
				if(sTask3 == 0 && gameMode == 2) {
				getPA().sendFrame126("@or1@Visit the ::train area @or2@", 7383);
				}
				if(sTask3 == 1 && gameMode == 2) {
				getPA().sendFrame126("@gre@Visit the ::train area", 7383);
				}
				if(sTask4 == 0)
				getPA().sendFrame126("@or1@Kill a player @or2@", 7336);
				if(sTask4 == 1)
				getPA().sendFrame126("@gre@Kill a player", 7336);
				if(sTask5 == 0)
				getPA().sendFrame126("@or1@Visit the slayer tower @or2@", 7339);
				if(sTask5 == 1)
				getPA().sendFrame126("@gre@Visit the slayer tower", 7339);
				if(sTask6 == 0)
				getPA().sendFrame126("@or1@Change your spellbook @or2@", 7338);
				if(sTask6 == 1)
				getPA().sendFrame126("@gre@Change your spellbook", 7338);
				if(playerMagicBook == 0)
				getPA().sendFrame126("@or1@  [@cya@Magic: @gre@Modern@or1@]", 7340);
				if(playerMagicBook == 1)
				getPA().sendFrame126("@or1@  [@cya@Magic: @gre@Ancient@or1@]", 7340);
				if(playerMagicBook == 2)
				getPA().sendFrame126("@or1@  [@cya@Magic: @gre@Lunar@or1@]", 7340);
				if(sTask7 == 0)
				getPA().sendFrame126("@or1@Get a make-over", 7346);
				if(sTask7 == 1)
				getPA().sendFrame126("@gre@Get a make-over", 7346);
				if(sTask8 == 0)
				getPA().sendFrame126("@or1@Choose your god", 7341);
				if(sTask8 == 1)
				getPA().sendFrame126("@gre@Choose your god", 7341);
				if(allsTasks == 0)
				getPA().sendFrame126("@or1@Complete all the tasks @or2@", 7342);//3433 3463
				if(allsTasks == 1)
				getPA().sendFrame126("@gre@Complete all the tasks", 7342);//3433 3463
				getPA().sendFrame126("@gre@Shops | (::shop)", 7337);//421, 422
				getPA().sendFrame126("@gre@Bosses | (::boss)", 7343);//941
				getPA().sendFrame126("@gre@PK Zones | (::PK)", 7335);//5571 npc id: 5321 5568 id: 5312
				getPA().sendFrame126("@gre@Minigames | (::MG)", 7344);//anim: 5485 npc: 5337
				getPA().sendFrame126("@gre@Slayer Tower | (::slayer)", 7345);
				getPA().sendFrame126("@or1@[@red@Skip Starter Tasks@or1@]", 7347);
				getPA().sendFrame126("", 7348);
				getPA().sendFrame126("", 12772);
				getPA().sendFrame126("", 673);
				getPA().sendFrame126("", 7352);
				getPA().sendFrame126("", 17510);
				getPA().sendFrame126("", 7353);
				getPA().sendFrame126("", 12129);
				getPA().sendFrame126("", 8438);
				getPA().sendFrame126("", 12852);
				getPA().sendFrame126("", 15841);
			}
		}
		
		public void addStarter() {
		if (!Connection.hasRecieved1stStarter(Server.playerHandler.players[playerId].connectedFrom) && !Connection.hasRecievedStarterName(Server.playerHandler.players[playerId].playerName)) {
					Connection.addIpToStarter1(Server.playerHandler.players[playerId].connectedFrom);
					Connection.addIpToStarterNames(Server.playerHandler.players[playerId].playerName);
					sTask1 = 0;
					sTask2 = 0;
					sTask3 = 0;
					sTask4 = 0;
					sTask5 = 0;
					sTask6 = 0;
					sTask7 = 0;
					sTask8 = 0;
					allsTasks = 0;
					doingStarter = 1;
		} else if (Connection.hasRecieved1stStarter(Server.playerHandler.players[playerId].connectedFrom) && !Connection.hasRecieved2ndStarter(Server.playerHandler.players[playerId].connectedFrom) && !Connection.hasRecievedStarterName(Server.playerHandler.players[playerId].playerName)) {
					Connection.addIpToStarter2(Server.playerHandler.players[playerId].connectedFrom);
					Connection.addIpToStarterNames(Server.playerHandler.players[playerId].playerName);
					sTask1 = 0;
					sTask2 = 0;
					sTask3 = 0;
					sTask4 = 0;
					sTask5 = 0;
					sTask6 = 0;
					sTask7 = 0;
					sTask8 = 0;
					allsTasks = 0;
					doingStarter = 1;
		}
	}

	public int maxPotential() {
		int total = 300;
		if(totalRisk() > 100000000)
			total += 150;
		else if(totalRisk() > 10000000)
			total += 75;
		else if(totalRisk() > 2000000)
			total += 30;

		if(redSkull == 1)
			total += 150;

		return total;
	}

		public void process() {
		//RuneTopList.runPendingCallbacks(playerName, this);
		
		/*if(inVarrockBank()) {
			getPA().showOption(4, 0,"Box", 3);
		} else {
			getPA().showOption(4, 0,"Trade With", 3);
		}*/

		if(bloodDamage > 0) {
		int theDmg = (int)(bloodDamage * .1);
			if(specAmount >= .25 && playerLevel[3] > theDmg) {
				specAmount -= .25;
				getItems().addSpecialBar(playerEquipment[playerWeapon]);
				if(bloodDamage < 60)
					bloodDamage += 2.5;
				if(!getHitUpdateRequired()) {
					setHitUpdateRequired(true);
					setHitDiff(theDmg);
					updateRequired = true;
					playerLevel[3] -= theDmg;
					getPA().refreshSkill(3);
				}
			} else {
				usingSpecial = false;
				getItems().addSpecialBar(playerEquipment[playerWeapon]);
				bloodDamage = 0;
				forcedText = "Ahhh... much better.";
				updateRequired = true;
				forcedChatUpdateRequired = true;
			}
		}

		if(getPand().inMission()) {
			if(!inPandArea()) {
				sendMessage("[@red@Sergeant Damien@bla@] @dre@Get back on the battlefield!");
				getPA().movePlayer(2802, 4718, instancedHeightLevel);
			}
		}

		if((stuckTimer > 0 && stuckTimer < 121) || stuckTimer > 121) {
			stuckTimer--;
			if(stuckTimer == 0) {
				sendMessage("You are teleported back to Safety.");
				getPA().startTeleport2(3087, 3497, 0);
				stuckTimer = 7200;
			}
		}
		if(damageTimer > 0) {
			damageTimer--;
			if(damageTimer <= 0)
				getPA().resetDamageDone();
		}
		if(allsTasks == 0 && (sTask1 == 1 && sTask2 == 1 && sTask3 == 1 && sTask4 == 1 && sTask5 == 1 && sTask6 == 1 && sTask7 == 1 && sTask8 == 1)) {
			if(getItems().freeSlots() > 2){
				allsTasks = 1;
				sendMessage("You have completed all of the starter tasks!");
				getItems().addItem(995,2500000);
				getItems().addItem(4151,1);
				doingStarter = 0;
				getPA().requestUpdates();
			} else {
				sendMessage("You need more inventory space for your reward.");
			}
		}
		if(foodTimer > 0)
			foodTimer--;
		if(potTimer > 0)
			potTimer--;
		if(offerDelay > 0)
			offerDelay--;
		if(freezeTimer > 0)
			stopMovement();
		if(isJailed == 1) {
			getPA().movePlayer(2608, 3160, 4);
			sendMessage("You are still in jail. Please wait for a staff member.");
		}
		
		if (getItems().playerHasItem(773, 1) || (playerEquipment[playerRing] == 773)){
			hadpring = 1;
		}
		if (getItems().playerHasItem(19111, 1) || (playerEquipment[playerRing] == 19111)){
			hadTokHaar = 1;
		}
		
		if(redSkull > 0)
		headIconPk = 1;
		
		if(packetTimer > 0)
			packetTimer--;
		
		if(lastTrade > 0) {
			lastTrade--;
		} else {
			getTradeAndDuel().cachedItems = null;
			getTradeAndDuel().cachedItems2 = null;
		}

		if(inWild()){
			/*pkpTimer++;
			int myMaxPotential = maxPotential();
				if(pkpTimer >= 50){//3 minutes (300 x 600 milisec = 180000 milisec)
					pkpTimer = 0;
					if(inHotspot(1)){
						if(potential >= myMaxPotential) {
							if(potential != myMaxPotential)
								sendMessage("@red@You have reached maximum potential and cannot go any higher.");
						potential = myMaxPotential;
						} else {
							potential += 25;
							if(Misc.random(1) == 0)
								sendMessage("@blu@You have gained 25% potential for standing in a low hotspot.");
						}
					} else if (inHotspot(2)){
						if(potential >= myMaxPotential) {
							if(potential != myMaxPotential)
								sendMessage("@red@You have reached maximum potential and cannot go any higher.");
							potential = myMaxPotential;
						} else {
							potential += 30;
							if(Misc.random(1) == 0)
								sendMessage("@blu@You have gained 30% potential for standing in a medium hotspot.");
						}
					} else if (inHotspot(3)){
					if(potential >= myMaxPotential) {
							if(potential != myMaxPotential)
								sendMessage("@red@You have reached maximum potential and cannot go any higher.");
						potential = myMaxPotential;
						} else {					
							potential += 35;
							if(Misc.random(1) == 0)
								sendMessage("@red@You have gained 35% potential for being in a major hotspot.");
						}
					} else {
					if(potential >= myMaxPotential) {
							if(potential != myMaxPotential)
								sendMessage("@red@You have reached maximum potential and cannot go any higher.");
						potential = myMaxPotential;
						} else {
							potential += 15;
							if(Misc.random(2) == 0)
								sendMessage("@red@You have gained 15% potential. Gain additional potential by standing in a hotspot.");
						}
					}
					questTab();
				}*/
		} else {
			if(safeTimer <= -2){
				getPA().resetDamageDone();
				/*if(pkpTimer >= 50){
					sendMessage("@red@Your hotspot timer is cleared as you exit the wilderness.");
				}*/
				if(wStreak >= 2){
					sendMessage("[@red@Streak@bla@] Your wilderness streak ends at @red@"+wStreak+"@bla@ as you exit the wilderness.");
					sendMessage("[@red@Streak@bla@] Wilderness killstreak bonus: @blu@+"+(wStreak * 5)+"@bla@ PK Points.");
					pkPoints += (wStreak * 5);
					}
			pkpTimer = 0;
			wStreak = 0;
			}
		}

		if (playerEquipment[playerWeapon] == -1) {
		getPA().resetAutocast();
		autocasting = false;
		}

		if(chatClickDelay > 0)
			chatClickDelay--;

		if(myTarget >= 0){
			Client c2 = (Client) Server.playerHandler.players[myTarget];
			if(c2 == null){
				myTarget = -1;
				target = 0;
				targetName = "None";
				getPA().createPlayerHints(10, -1);
				sendMessage("@red@Your target is reset because your target is offline.");
			}
		}
		
		if(targetFinderDelay > 0 && inWild() && target == 0){
			targetFinderDelay--;
			if(playerRights == 3)
				sendMessage("Debug message: targetFinderDelay = " + targetFinderDelay);
		}
			
		if(targetFinderDelay <= 0){
			if(inWild() && !inFunPk() && !inStakeArena() && !inPits && target == 0 && targetSystem == 1){
				findTarget();
			}
		}
		
		if(duelStatus == 5){
		Client o = (Client) Server.playerHandler.players[duelingWith];
		if(o == null) {
			getTradeAndDuel().duelVictory();
		}
		}
		
		if(crystalBowSpecTimer > 0)
		{
			if(crystalBowSpecTimer == 1)
				sendMessage("@red@Your crystal bow is no longer enraged.");
			crystalBowSpecTimer--;
		}
		
		if(crystalBowSpecTimer > 50)
			crystalBowSpecTimer = 50;
		


		if(System.currentTimeMillis() - saveGameDelay > Config.SAVE_TIMER && !disconnected && !inTrade && !inSecondWindow) {
			if(isFlagged == 2 && packetLogString != "null"){
				try{
					
					packetLog.open(playerName);
					packetLog.print(packetLogString);
					packetLog.close();

					packetLogString = "null";
				} catch (Exception e){
					System.out.println("Error saving log packets for " + playerName);
				}
			}
		
			SaveGame();
			saveCharacter = true; 
			saveGameDelay = System.currentTimeMillis();
		}
		
		SPTimer++;
		if(SPTimer == 300){
			//if(SP < 1000) {

				int pointsGained = 500 + (memberStatus * 250);
				int maxPoints = 500 + (memberStatus * 1000);

				//sendMessage("[@red@Server@bla@]: Your Gear Points increase by @blu@"+pointsGained+"@bla@. Spend them at a Legends Guard.");
				
				if(announceOn == 0)
					sendMessage(serverMessage[new java.util.Random().nextInt(serverMessage.length)]);

				SP += pointsGained;
				
				if(SP > maxPoints) {
					SP = maxPoints;
				}

				questTab();
			//}
			SPTimer = 0;
		}
		/*if(clawPlayerDelay > 0) { //a player hit method, can't remember, but an event
			clawPlayerDelay--;
			if (clawPlayerDelay == 0) {
				getCombat().applyPlayerHit(lastAttacked, clawDamage[1]);
				getCombat().applyPlayerHit(lastAttacked, clawDamage[2]);
			}
		}*/
		
		/*if(clawNPCDelay > 0) { //npc delayed hit method event
			clawNPCDelay--;
			if (clawNPCDelay == 0) {
				getCombat().applyNpcMeleeDamage2(lastNpcAttacked, 2, clawDamage[1]);
				getCombat().applyNpcMeleeDamage2(lastNpcAttacked, 2, clawDamage[2]);
			}
		}*/
		
		/*if(yellTimer >= 0)
		yellTimer--;*/
		
		killsThisMinuteTimer--;
		
		if(killsThisMinuteTimer <= 0){
		killsThisMinuteTimer = 240;
		killsThisMinute = 0;
		}
		
		if(cantGetKillsTimer > 0)
		cantGetKillsTimer--;
		
		if(cantGetKillsTimer == 0)
		cantGetKills = 0;

//lastvote--;
             if(inWild() && (underAttackBy != 0 || underAttackBy2 > 0)) {
             	safeTimer = 22;
             }
             if(safeTimer >= -8) {
                safeTimer--;
             }







               /* if(inWild() && (underAttackBy != 0 || underAttackBy2 > 0)) {
                        safeTimer = 25;
                }
                 if(safeTimer > 0 && !inWild()) {
                       safeTimer--;
                }*/


		
		if (smeltTimer > 0 && smeltType > 0) {
			smeltTimer--;
		} else if (smeltTimer == 0 && smeltType > 0) {
			getSmithing().smelt(smeltType);
		} else if (fishing && fishTimer > 0) {
			fishTimer--;
		} else if (fishing && fishTimer == 0) {
			getFishing().catchFish();
		} else if (iscooking && cookTimer > 0) {
			cookTimer--;
		} else if (iscooking && cookTimer == 0) {
			getCooking().cookFish();
		}
		/*if (tradeTimer > 0) {
			tradeTimer--;
		}*/
		/*if (absX == 3292 && absY == 3091 || absX == 3292 && absY == 3090) {
			getPA().walkTo3(-130, -64);
		}
		if (absX == 3274 && absY == 3072 || absX == 3275 && absY == 3073) {
			getPA().walkTo3(-130, -64);
		}
		if (absX == 3256 && absY == 3054 || absX == 3257 && absY == 3055) {
			getPA().walkTo3(-130, -64);
		}*/
/*
		if(clawDelay > 0) {
			clawDelay--;
		}

		if(clawDelay == 1) {
		double damage4 = 0;
			if(npcIndex > 0) {
				getCombat().applyNpcMeleeDamage(npcIndex, 1, previousDamage / 2);
			}
			if(playerIndex > 0) {
				getCombat().applyPlayerMeleeDamage(playerIndex, 1, previousDamage / 2);
			}
			damage4 = previousDamage % 2;
			if(damage4 >= 0.001) {
				previousDamage = previousDamage + 1;
				damage4 = 0;
			}
			if(npcIndex > 0) {
				getCombat().applyNpcMeleeDamage(npcIndex, 2, previousDamage);
			}
			if(playerIndex > 0) {
				getCombat().applyPlayerMeleeDamage(playerIndex, 2, previousDamage);
			}
			clawDelay = 0;
			specEffect = 0;
			previousDamage = 0;
			usingClaws = false;
		}
*/
		
		/*if (System.currentTimeMillis() - lastPoison > 20000 && poisonDamage > 0) { //poison in appendPoison method for an event
			int damage = poisonDamage/2;
			if (damage > 0) {
				if (!getHitUpdateRequired()) {
					setHitUpdateRequired(true);
					setHitDiff(damage);
					updateRequired = true;

					poisonMask = 1;
				} else if (!getHitUpdateRequired2()) {
					setHitUpdateRequired2(true);
					setHitDiff2(damage);
					updateRequired = true;
					poisonMask = 2;
				}
				lastPoison = System.currentTimeMillis();
				poisonDamage--;
				dealDamage(damage);
getPA().refreshSkill(3);
			} else {
				poisonDamage = -1;
				sendMessage("You are no longer poisoned.");
			}	
		}*/
		/*if(System.currentTimeMillis() - duelDelay > 800 && duelCount > 0) { //removed this, duel doesnt even work lol
			if(duelCount != 1) {
stopMovement();
				forcedChat(""+(--duelCount));
				duelDelay = System.currentTimeMillis();
			} else {
				damageTaken = new int[Config.MAX_PLAYERS];
				forcedChat("FIGHT!");
				duelCount = 0;
			}
		}*/
	
		if(worshippedGod == 2 && godReputation >= 100){
			if(System.currentTimeMillis() - specDelay > Config.INCREASE_SPECIAL_AMOUNT_ZAM) {
				specDelay = System.currentTimeMillis();
				if(specAmount < 10) {
					specAmount += .5;
		
					if (specAmount > 10)
						specAmount = 10;
					getItems().addSpecialBar(playerEquipment[playerWeapon]);
				}
			}
		} else {
			if(System.currentTimeMillis() - specDelay > Config.INCREASE_SPECIAL_AMOUNT) {
				specDelay = System.currentTimeMillis();
				if(specAmount < 10) {
					specAmount += .5;
		
					if (specAmount > 10)
						specAmount = 10;
					getItems().addSpecialBar(playerEquipment[playerWeapon]);
				}
			}
		}
		
		//clickObject class event
		/*if(clickObjectType > 0 && goodDistance(objectX + objectXOffset, objectY + objectYOffset, getX(), getY(), objectDistance)) {
			if(clickObjectType == 1) {
				getActions().firstClickObject(objectId, objectX, objectY);
			}
			if(clickObjectType == 2) {
				getActions().secondClickObject(objectId, objectX, objectY);
			}
			if(clickObjectType == 3) {
				getActions().thirdClickObject(objectId, objectX, objectY);
			}
		}*/
		
		if((clickNpcType > 0) && Server.npcHandler.npcs[npcClickIndex] != null) {			
			if(goodDistance(getX(), getY(), Server.npcHandler.npcs[npcClickIndex].getX(), Server.npcHandler.npcs[npcClickIndex].getY(), 1)) {
				if(clickNpcType == 1) {
					turnPlayerTo(Server.npcHandler.npcs[npcClickIndex].getX(), Server.npcHandler.npcs[npcClickIndex].getY());
					Server.npcHandler.npcs[npcClickIndex].facePlayer(playerId);
					getActions().firstClickNpc(npcType);
				}
				if(clickNpcType == 2) {
					turnPlayerTo(Server.npcHandler.npcs[npcClickIndex].getX(), Server.npcHandler.npcs[npcClickIndex].getY());
					Server.npcHandler.npcs[npcClickIndex].facePlayer(playerId);
					getActions().secondClickNpc(npcType);
				}
				if(clickNpcType == 3) {
					turnPlayerTo(Server.npcHandler.npcs[npcClickIndex].getX(), Server.npcHandler.npcs[npcClickIndex].getY());
					Server.npcHandler.npcs[npcClickIndex].facePlayer(playerId);
					getActions().thirdClickNpc(npcType);
				}
			}
		}
		
		if(walkingToItem) {
			if(getX() == pItemX && getY() == pItemY || goodDistance(getX(), getY(), pItemX, pItemY,1)) {
				walkingToItem = false;
				Server.itemHandler.removeGroundItem(this, pItemId, pItemX, pItemY, true);
			}
		}
		
		if(followId > 0 && !inDuelArena() && !getPA().inPitsWait()) {
			getPA().followPlayer();
		} else if (followId2 > 0) {
			getPA().followNpc();
		}
		
		getCombat().handlePrayerDrain();
		
		if(System.currentTimeMillis() - singleCombatDelay >  7000) {
			underAttackBy = 0;
		}
		if (System.currentTimeMillis() - singleCombatDelay2 > 7000) {
			underAttackBy2 = 0;
		}
		
		if(System.currentTimeMillis() - restoreStatsDelay > 90000) { //original 60000
			restoreStatsDelay = System.currentTimeMillis();
			for (int level = 0; level < playerLevel.length; level++)  {
				if (playerLevel[level] < getLevelForXP(playerXP[level])) {
					if(level != 5) { // prayer doesn't restore
						playerLevel[level] += 1;
						getPA().setSkillLevel(level, playerLevel[level], playerXP[level]);
						getPA().refreshSkill(level);
					}
				} else if (playerLevel[level] > getLevelForXP(playerXP[level])) {
					playerLevel[level] -= 1;
					getPA().setSkillLevel(level, playerLevel[level], playerXP[level]);
					getPA().refreshSkill(level);
				}
			}
		}

		/*if(System.currentTimeMillis() - teleGrabDelay >  1550 && usingMagic) {
			usingMagic = false;
			if(Server.itemHandler.itemExists(teleGrabItem, teleGrabX, teleGrabY)) {
				Server.itemHandler.removeGroundItem(this, teleGrabItem, teleGrabX, teleGrabY, true);
			}
		}*/
		

		if(inWild()) {
			int higher = combatLevel + wildLevel;
			int lower = combatLevel - wildLevel;
			if (higher > 126){
			higher = 126;
			}
			if (lower < 3){
			lower = 3;
			}
			int modY = absY > 6400 ?  absY - 6400 : absY;
			if(!(absX >= 3079 && absX <= 3134 && absY > 9990 && absY <= 9924) && !(absX > 2350 && absX < 2440 && absY > 4700 && absY < 4740) && !(absX > 3207 && absX < 3221 && absY > 3422 && absY < 3437) && !(absX > 2941 && absX < 3060 && absY > 3314 && absY < 3399) && !(absX > 2583 && absX < 2729 && absY > 3255 && absY < 3343)) {
			wildLevel = (((modY - 3520) / 8) + 2);
			}
			if((absX >= 3079 && absX <= 3134 && absY > 9990 && absY <= 9924))
			wildLevel = 5;
			if((absX > 3013 && absX < 3020 && absY > 3630 && absY < 3633) || (absX > 3017 && absX < 3041 && absY > 3620 && absY < 3641))
			wildLevel = 0;
			if(inVarrock())
			wildLevel = 10;
			if(inBlackDLair())
			wildLevel = 52;
			if(inKBD())
			wildLevel = 50;
			if(inDag())
			wildLevel = 56;
			if(absX > 2350 && absX < 2440 && absY > 4700 && absY < 4740)
			wildLevel = 25;
			if(absX > 3013 && absX < 3065 && absY > 10295 && absY < 10360)
			wildLevel = 52;
			
			getPA().walkableInterface(197);
			if(!inHotspot(1) && !inHotspot(2) && !inHotspot(3) && wildLevel > 0)
				getPA().sendFrame126("@yel@"+lower+" - "+higher, 199);
			if(inHotspot(1))
				getPA().sendFrame126("@or1@"+lower+" - "+higher, 199);
			if(inHotspot(2))
				getPA().sendFrame126("@or2@"+lower+" - "+higher, 199);
			if(inHotspot(3))
				getPA().sendFrame126("@red@"+lower+" - "+higher, 199);
			if(!inMulti())
				getPA().multiWay(-1);
			if(wildLevel >= 0 || safeTimer >= 0)
				getPA().showOption(3, 0, "Attack", 1);

		} else if (!inWild() && safeTimer > 0){
			getPA().walkableInterface(197);
			wildLevel = (5);
			getPA().showOption(3, 0, "Attack", 1);
			int kkk = (int)safeTimer/2;
			getPA().sendFrame126("@red@"+kkk, 199);
		} else if (inFunPk() || inStakeArena()) { 
			wildLevel = (123);
			getPA().showOption(3, 0, "Attack", 1);
			getPA().sendFrame126("@gre@Safe", 199);
			getPA().walkableInterface(197);
		} else if (isInHighRiskPK()) {
		int higher = combatLevel + wildLevel;
			int lower = combatLevel - wildLevel;
			if (higher > 126){
			higher = 126;
			}
			if (lower < 3){
			lower = 3;
			}
			wildLevel = (20);
			getPA().showOption(3, 0, "Attack", 1);
			getPA().sendFrame126("@or2@"+lower+" - "+higher, 199);
			getPA().walkableInterface(197);
		} else if (inFaladorPvP()) {
			wildLevel = (20);
			int higher = combatLevel + wildLevel;
			int lower = combatLevel - wildLevel;
			if (higher > 126){
			higher = 126;
			}
			if (lower < 3){
			lower = 3;
			}
			getPA().showOption(3, 0, "Attack", 1);
			getPA().sendFrame126("@or2@"+lower+" - "+higher, 199);
			getPA().walkableInterface(197);
		} else if (inDuelArena()) {
			getPA().walkableInterface(201);
			if(duelStatus == 5) {
				getPA().showOption(3, 0, "Attack", 1);
			} else {
				getPA().showOption(3, 0, "Challenge", 1);
			}
		} else if(inBarrows()){
			getPA().sendFrame126("Kill Count: "+barrowsKillCount, 4536);
			getPA().walkableInterface(4535);
		} else if (inCwGame || inPits) {
			getPA().showOption(3, 0, "Attack", 1);	
		} else if (getPA().inPitsWait()) {
			getPA().showOption(3, 0, "Null", 1);
		} else if (getPand().inMission()){ 
			int seconds = (getPand().ticksPlayed * 600) / 1000;
			getPA().sendFrame126("          @whi@Kills: @red@" + getPand().killCount + "  @whi@Points: @red@" + getPand().totalPointsEarned, 6570);
			getPA().sendFrame126("@cya@    Time: " + seconds, 6572); 
			getPA().sendFrame126(" ", 6664); 
			getPA().walkableInterface(6673);
		} else if (!inCwWait) { 
			getPA().sendFrame99(0);
			getPA().walkableInterface(-1);
			getPA().showOption(3, 0, "Null", 1);
		}
		
		if(!hasMultiSign && inMulti()) {
			hasMultiSign = true;
			getPA().multiWay(1);
		}
		
		if(hasMultiSign && !inMulti()) {
			hasMultiSign = false;
			getPA().multiWay(-1);
		}

		if(skullTimer > 0) {
			skullTimer--;
			
			if(skullTimer == 1) {
				isSkulled = false;
				attackedPlayers.clear();
				headIconPk = -1;
				skullTimer = -1;
				redSkull = -1;
				getPA().requestUpdates();
			}	
		}
		
		if(isDead && respawnTimer == -6) {
			getPA().applyDead();
		}
		
		if(respawnTimer == 7) {
			respawnTimer = -6;
			getPA().giveLife();
		} else if(respawnTimer == 12) {
			respawnTimer--;
			startAnimation(0x900);
			poisonDamage = -1;
		}	
		
		if(respawnTimer > -6) {
			respawnTimer--;
		}
		if(freezeTimer > -6) {
			freezeTimer--;
			if (frozenBy > 0) {
				if (Server.playerHandler.players[frozenBy] == null) {
					freezeTimer = -1;
					frozenBy = -1;
				} else if (!goodDistance(absX, absY, Server.playerHandler.players[frozenBy].absX, Server.playerHandler.players[frozenBy].absY, 20)) {
					freezeTimer = -1;
					frozenBy = -1;
				}
			}
		}
		
		if(hitDelay > 0) {
			hitDelay--;
		}

		ladderTimer--;
		

		if(teleTimer > 0) {
			teleTimer--;
			if (!isDead) {
				if(teleTimer == 1 && newLocation > 0) {
					teleTimer = 0;
					getPA().changeLocation();
				}
				if(teleTimer == 5) {
					teleTimer--;
					getPA().processTeleport();
				}
				if(teleTimer == 9 && teleGfx > 0) {
					teleTimer--;
					if (teleGfx == 678) {
					gfx0(teleGfx);
					} else {
					gfx100(teleGfx);
					}
				}
			} else {
				teleTimer = 0;
			}
		}	

		if(hitDelay == 1) {
			if(oldNpcIndex > 0) {
				getCombat().delayedHit(oldNpcIndex);
			}
			if(oldPlayerIndex > 0) {
				getCombat().playerDelayedHit(oldPlayerIndex);				
			}		
		}
		
		if(attackTimer > 0) {
			attackTimer--;
		}
		
		
		
		/*if(yellDelay > 0) { //commands.java yell statement event
			yellDelay--;
		}
		
		if(specRestore > 0) {
			specRestore --;
		}*/

		/*if(clawPlayerDelay > 0) {
			clawPlayerDelay--;
			if (clawPlayerDelay == 0) {
				delayedDamage = clawDamage[1];
				delayedDamage2 = clawDamage[2];
				getCombat().applyPlayerHit(lastAttacked, clawDamage[1]);
				getCombat().applyPlayerHit(lastAttacked, clawDamage[2]);
			}
		}*/
		
		/*if(clawNPCDelay > 0) {
			clawNPCDelay--;
			if (clawNPCDelay == 0) {
				getCombat().applyNpcMeleeDamage(lastNpcAttacked, 1, clawDamage[1]);
				getCombat().applyNpcMeleeDamage(lastNpcAttacked, 2, clawDamage[2]);
			}
		}Dick*/
				
				/*if(morrJavTimer > 0) {
					morrJavTimer--;
					if (!isDead) {
						if (morrJavTimer == 0) {
							if (morrJavEffect > 0) {
								int morrJavDamage = 5;
								if (morrJavEffect >= 5) {
									morrJavEffect -= 5;
								} else {
									morrJavDamage = morrJavEffect;
									morrJavEffect = 0;
								}
								if (Server.playerHandler.players[playerId].playerLevel[3] - morrJavDamage < 0) {
									morrJavDamage = Server.playerHandler.players[playerId].playerLevel[3];
									morrJavEffect = 0;
								}
								Server.playerHandler.players[playerId].logoutDelay = System.currentTimeMillis();
								Server.playerHandler.players[playerId].dealDamage(morrJavDamage);
								Server.playerHandler.players[playerId].updateRequired = true;
								getPA().refreshSkill(3);
								Server.playerHandler.players[playerId].handleHitMask(morrJavDamage);
								if (morrJavEffect > 0) {
									morrJavTimer = 3;
								}
							} else {
								morrJavTimer = 0;
								morrJavEffect = 0;
							}
						}
					}
				}*/
		
		if(attackTimer == 1){
			if(npcIndex > 0 && clickNpcType == 0) {
				getCombat().attackNpc(npcIndex);
			}
			if(playerIndex >= 0) {
				getCombat().attackPlayer(playerIndex);
			}
		} else if (attackTimer <= 0 && (npcIndex > 0 || playerIndex > 0)) {
			if (npcIndex > 0) {
				attackTimer = 0;
				getCombat().attackNpc(npcIndex);
			} else if (playerIndex > 0) {
				attackTimer = 0;
				getCombat().attackPlayer(playerIndex);
			}
		}
		
		if(timeOutCounter > Config.TIMEOUT) {
			disconnected = true;
		}
		
		timeOutCounter++;
		
		if(inTrade && tradeResetNeeded){
			Client o = (Client) Server.playerHandler.players[tradeWith];
			if(o != null){
				if(o.tradeResetNeeded){
					getTradeAndDuel().resetTrade();
					o.getTradeAndDuel().resetTrade();
				}
			}
		}
	}
	
	public void setCurrentTask(Future<?> task) {
		currentTask = task;
	}

	public Future<?> getCurrentTask() {
		return currentTask;
	}
	
	public synchronized Stream getInStream() {
		return inStream;
	}
	
	public synchronized int getPacketType() {
		return packetType;
	}
	
	public synchronized int getPacketSize() {
		return packetSize;
	}
	
	public synchronized Stream getOutStream() {
		return outStream;
	}
	
	public ItemAssistant getItems() {
		return itemAssistant;
	}
		
	public PlayerAssistant getPA() {
		return playerAssistant;
	}

	public TradeLog getTradeLog() {
		return tradeLog;
	}

	
	public DialogueHandler getDH() {
		return dialogueHandler;
	}
	
	public ShopAssistant getShops() {
		return shopAssistant;
	}
	
	public TradeAndDuel getTradeAndDuel() {
		return tradeAndDuel;
	}

	public Boxing getBoxing() {
		return boxing;
	}
	
	public CombatAssistant getCombat() {
		return combatAssistant;
	}
	
	public ActionHandler getActions() {
		return actionHandler;
	}
  
	public PlayerKilling getKill() {
		return playerKilling;
	}
	
	public IoSession getSession() {
		return session;
	}
	
	public Potions getPotions() {
		return potions;
	}
	
	public Pins getBankPin() {
		return pins;
	}
	
	public PotionMixing getPotMixing() {
		return potionMixing;
	}
	
	public Food getFood() {
		return food;
	}
	
	/**
	 * Skill Constructors
	 */
	public Slayer getSlayer() {
		return slayer;
	}
	
	public Runecrafting getRunecrafting() {
		return runecrafting;
	}
	
	public Woodcutting getWoodcutting() {
		return woodcutting;
	}
	
	public Mining getMining() {
		return mine;
	}
	
	public Cooking getCooking() {
		return cooking;
	}
	
	public Agility getAgility() {
		return agility;
	}
	
	public Fishing getFishing() {
		return fish;
	}
	
	public Crafting getCrafting() {
		return crafting;
	}
	
	public Smithing getSmithing() {
		return smith;
	}
	
	public Farming getFarming() {
		return farming;
	}
	
	public Thieving getThieving() {
		return thieving;
	}
	
	public Herblore getHerblore() {
		return herblore;
	}
	
	public Firemaking getFiremaking() {
		return firemaking;
	}
	
	public SmithingInterface getSmithingInt() {
		return smithInt;
	}
	
	public Prayer getPrayer() { 
		return prayer;
	}
	
	
	public Pandemonium getPand(){
		return pandemonium;
	}

	public BroodooBrothers getBroodoo()
	 {
	  return broodoo;
	 }
	
	public Fletching getFletching() { 
		return fletching;
	}
	
	/**
	 * End of Skill Constructors
	 */
	
	public void queueMessage(Packet arg1) {
		synchronized(queuedPackets) {
			//if (arg1.getId() != 41)
				queuedPackets.add(arg1);
			//else
				//processPacket(arg1);
		}
	}
	
	public synchronized boolean processQueuedPackets() {
		Packet p = null;
		synchronized(queuedPackets) {
			p = queuedPackets.poll();
		}
		if(p == null) {
			return false;
		}
		inStream.currentOffset = 0;
		packetType = p.getId();
		packetSize = p.getLength();
		inStream.buffer = p.getData();
		if(packetType == 3 && packetSize == 1)
			return false;
		if(packetType > 0) {
			//sendMessage("PacketType: " + packetType);
			if(playerRights >= 3)
			printPacketLog("Player processed packet: " + packetType + " size: "+packetSize+".");
			PacketHandler.processPacket(this, packetType, packetSize);//e
		}
		timeOutCounter = 0;
		return true;
	}
	
	public synchronized boolean processPacket(Packet p) {
		//synchronized (this) {
			if(p == null) {
				return false;
			}
			inStream.currentOffset = 0;
			packetType = p.getId();
			packetSize = p.getLength();
			inStream.buffer = p.getData();
			if(packetType == 3 && packetSize == 1)
				return false;
			if(packetType > 0) {
				//sendMessage("PacketType: " + packetType);
				if(playerRights >= 3)
					printPacketLog("Player processed packet: " + packetType + " size: "+packetSize+".");
				PacketHandler.processPacket(this, packetType, packetSize);
			}
			timeOutCounter = 0;
			return true;
		//}
	}
	
	
	public void correctCoordinates() {
		if (inPcGame()) {
			getPA().movePlayer(2657, 2639, 0);
		}
		}


}




