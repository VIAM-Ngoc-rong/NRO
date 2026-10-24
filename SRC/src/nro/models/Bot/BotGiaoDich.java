package nro.models.Bot;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import nro.models.item.Item;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.services.PlayerService;
import nro.models.services.ChatGlobalService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services_func.Trade;

public class BotGiaoDich {
   public int idItem;
   public int idItTd;
   public int slot;
   
   private long lastimeChat;
   private long lastimeChatTrain;
   private Trade trade;
   
   public Bot bot;
   private Player pl;
   
   public BotGiaoDich(int item , int traodoi , int slot){
        this.idItem = item;
        this.idItTd = traodoi;
        this.slot = slot;
   }
   
   public BotGiaoDich(BotGiaoDich shop){
        this.idItem = shop.idItem;
        this.idItTd = shop.idItTd;
        this.slot = shop.slot;
   }
   
   public void update(){
       this.mapL();
       this.chat();
   }
   
   public String getChat(){
        if (this.bot == null || this.bot.zone == null || this.bot.zone.map == null) {
            return "";
        }
        var t1 = ItemService.gI().getTemplate(this.idItem);
        var t2 = ItemService.gI().getTemplate(this.idItTd);
        String name1 = t1 != null ? t1.name : String.valueOf(this.idItem);
        String name2 = t2 != null ? t2.name : String.valueOf(this.idItTd);
        return String.format("p %s ja x%d %s/1 %s k%d", name1, this.slot, name2, this.bot.zone.map.mapName, this.bot.zone.zoneId);
   }
   
   public void chat(){
       if (this.lastimeChat < (System.currentTimeMillis() - ((100 + new Random().nextInt(100)) * 1000))){
       ChatGlobalService.gI().chat1(this.bot , this.getChat());
       this.lastimeChat = System.currentTimeMillis();
      }
     if (this.lastimeChatTrain < (System.currentTimeMillis() - ((5 + new Random().nextInt(5)) * 1000))){
      Service.gI().chat(this.bot , getChat());
      this.lastimeChatTrain = System.currentTimeMillis();
     }
   }
   
   
   public void activeTraDe(Player pl){
      trade = new Trade(pl , bot);
      this.pl = pl;
      this.trade.openTabTrade();
   }
  
   public void CheckTraDe(List<Item> item){
    int slot1 = item.stream()
    .filter(it -> it.template.id == this.idItTd && it.quantity >= this.slot)
    .mapToInt(it -> it.quantity)
    .findFirst()
    .orElse(0);
    boolean check = slot1 > 0;
     if (check){
         active(slot1);
     } else {
         this.trade.cancelTrade();
     }
   }
   
   public void active(int sl){
       int sl1 = (int) Math.round((double) sl / this.slot);
       Item it = ItemService.gI().createNewItem((short) this.idItem , sl1);
       this.trade.addItemBot(it);
       this.trade.lockTran(this.bot);
       this.trade.acceptTrade();
   }
   
   public void mapL(){
      if (this.bot == null) {
          return;
      }
      if(this.bot.zone == null || this.bot.zone.map == null || this.bot.zone.map.mapId != 84){
          Zone zone = this.bot.getRandomZone(84);
          if(zone != null){
              ChangeMapService.gI().goToMap(this.bot, zone);
              if (this.bot.zone != null) {
                  this.bot.zone.load_Me_To_Another(this.bot);
              }
              PlayerService.gI().playerMove(this.bot, 81 + new Random().nextInt(716), 336);
          }
      }
   }
}