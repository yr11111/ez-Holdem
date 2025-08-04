package com.yr.talkDice.ws;

import com.yr.talkDice.domain.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@ServerEndpoint("/server")
public class WsServer {

    private Session session;

    /**
     * 记录在线连接客户端数量
     */
    private static final AtomicInteger onlineCount = new AtomicInteger(0);

    private static final CopyOnWriteArrayList<WsServer> wsServers = new CopyOnWriteArrayList<>();

    private static Integer totalCount = 0;

    /**
     * 存放每个连接进来的客户端对应的websocketServer对象，用于后面群发消息
     */
    private static final Map<WsServer, Player> clientJetton = new ConcurrentHashMap<>();

    private List<String> pokerList = PokerListBean.getInstance().getPokerList();

    private String publicPokers;

    /**
     * 服务端与客户端连接成功时执行
     *
     * @param session 会话
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        onlineCount.incrementAndGet();
        wsServers.add(this);

        Player player = new Player();
        player.setOrder(getMaxOrder() + 1);
        player.setPokerList(new ArrayList<>());

        clientJetton.put(this, player);
        sendMessage("m连接服务器成功~!");
    }

    public int getMaxOrder() {

        int maxOrder = 0;
        List<Player> playerList = new ArrayList<>(clientJetton.values());
        if (CollectionUtils.isEmpty(playerList)) {
            return maxOrder;
        }
        for (Player player : playerList) {

            maxOrder = Math.max(maxOrder, player.getOrder());
        }
        return maxOrder;
    }

    /**
     * 收到客户端的消息时执行
     *
     * @param message 消息
     * @param session 会话
     */
    @OnMessage
    public synchronized void onMessage(String message, Session session) {

        log.info("收到来自客户端的消息，客户端地址：{}，消息内容：{}", session.getMessageHandlers(), message);

        //业务逻辑，对消息的处理
        if ("ag".equals(message)) {
            //洗牌
            pokerList = PokerDeck.shuffleDeck();
            publicPokers = "";

            totalCount = 0;
            for (WsServer ws : wsServers) {
                Player player = clientJetton.get(ws);

                if (!(player.getChip() > 0)) {
                    player.setWatch(1);
                    player.setFold(1);
                }

                if (player.getWatch() != 1) {
                    player.setFold(0);
                    String poppedCards = PokerDeck.popRandomElements(2, pokerList);
                    player.setCurrChip(1);
                    player.setChip(player.getChip() - 1);
                    player.setPokerList(new ArrayList<>());
                    ws.sendMessage("m底牌:" + poppedCards);
                    ws.sendMessage("$本局下注:" + player.getCurrChip());
                    totalCount++;

                    List<String> curPlayerPokerList = player.getPokerList();
                    curPlayerPokerList.addAll(Arrays.asList(poppedCards.split(",")));

                }
            }
            showPlayList();
            sendMessageToAll("$当前池数:" + totalCount);
        } else if ("turn".equals(message)) {

            String poppedCards = PokerDeck.popRandomElements(3, pokerList);
            sendMessageToAll("m公共牌:" + poppedCards);
            publicPokers = poppedCards;
        } else if ("follow".equals(message)) {

            String poppedCards = PokerDeck.popRandomElements(1, pokerList);
            sendMessageToAll("m公共牌:" + poppedCards);
            publicPokers += ",";
            publicPokers += poppedCards;
        } else if (message.startsWith("#")) {

            String[] split = message.split("#");
            Player player = clientJetton.get(this);
            player.setName("[" + split[1] + "]");
            if (split[2] == null || split[2].equals("") || split[2].equals(" ")) {
                split[2] = "0";
            }
            player.setChip(Integer.parseInt(split[2]));
            sendMessageToAll("#" + player.getName() + " chip:" + split[2]);
            showPlayList();
        } else if (message.startsWith("$")) {

            String[] split = message.split("\\$");
            int chip = Integer.parseInt(split[1]);
            totalCount += chip;
            Player player = clientJetton.get(this);
            player.setChip(player.getChip() - chip);
            int currChip = player.getCurrChip();
            currChip += chip;
            player.setCurrChip(currChip);
            showPlayList();
            sendMessageToAll("$" + player.getName() + "+" + split[1] + "--当前池数:" + totalCount);
            sendMessage("$--------------本局下注:" + player.getCurrChip());
        } else if ("fold".equals(message)) {

            Player player = clientJetton.get(this);
            player.setFold(1);
            sendMessageToAll("$" + player.getName() + ": fold");
            List<Player> playerList = new ArrayList<>(clientJetton.values());
            // 计数 fold 为 0 的 player 并保存这个 player
            Player targetPlayer = null;
            int foldZeroCount = 0;

            for (Player player1 : playerList) {
                if (player.getWatch() != 1 && player1.getFold() == 0) {
                    foldZeroCount++;
                    if (foldZeroCount > 1) {
                        // 如果 fold 为 0 的 player 数量大于 1，跳出循环
                        break;
                    }
                    targetPlayer = player1; // 保存这个 player
                }
            }

            if (foldZeroCount == 1) {
                int chip = targetPlayer.getChip();
                targetPlayer.setChip(chip + totalCount);
                sendMessageToAll("$所有人都龟了 " + targetPlayer.getName() + "+" + totalCount);
                showPlayList();
            }
        } else if (message.startsWith("all")) {
            String[] split = message.split("all");
            String content = split[1];
            Player player = clientJetton.get(this);

            sendMessageToAll("all" + player.getName() + ":" + content);
        } else if ("watch".equals(message)) {

            Player player = clientJetton.get(this);
            player.setWatch(1);
        } else if (message.startsWith("order")) {

            showPlayList();
        } else if ("open".equals(message)) {

            showPlayPoker();
        }

    }

    private void showPlayList() {
        List<Player> playerList = new ArrayList<>(clientJetton.values());
        List<Player> sortedPlayerList = playerList.stream()
                .sorted(Comparator.comparingInt(Player::getOrder))
                .collect(Collectors.toList());
        StringBuilder result = new StringBuilder();
        result.append("order");
        for (Player player : sortedPlayerList) {
            int chip = player.getChip();
            result.append(player.getOrder()).append("号").append(player.getName()).append(",筹码:").
                    append(chip);

            if (chip == 0) {
                result.append("    寄");
            }
            result.append("\n");
        }

        sendMessageToAll(result.toString());
    }

    private void showPlayPoker() {
        List<Player> playerList = new ArrayList<>(clientJetton.values());
        List<Player> sortedPlayerList = playerList.stream()
                .sorted(Comparator.comparingInt(Player::getOrder))
                .collect(Collectors.toList());
        StringBuilder result = new StringBuilder();
        result.append("open");
        result.append("公共牌:");
        result.append(publicPokers);
        result.append("\n");
        List<PokerHandEvaluator.Result> results = new ArrayList<>();
        Map<Player, List<String>> listPlayerMap = new HashMap<>();
        for (Player player : sortedPlayerList) {

            if (player.getWatch() != 1 && player.getFold() == 0) {

                result.append(player.getName()).append(":");

                List<String> pokerList = player.getPokerList();

                for (String poker : pokerList) {
                    result.append(poker).append("  ");
                }
                String[] split = publicPokers.split(",");
                pokerList.addAll(Arrays.asList(split));
                pokerList.replaceAll(String::trim);
                PokerHandEvaluator.Result bestPokerHand = PokerHandEvaluator.getBestPokerHand(pokerList);
                result.append("Best Hand: ").append(bestPokerHand.bestHand).append(", Hand Type: ").append(bestPokerHand.handTypeName);
                result.append("\n");
                results.add(bestPokerHand);
                listPlayerMap.put(player, bestPokerHand.bestHand);
            }

        }

        PokerHandEvaluator.Result bestOfBest = PokerHandEvaluator.getBestOfBestHands(results);

        if (bestOfBest.isTie) {

            List<Player> playList = new ArrayList<>();

            result.append("平手呗那就");
            result.append("\n");
            for (Player player : listPlayerMap.keySet()) {
                if (PokerHandEvaluator.areHandsEqual(listPlayerMap.get(player), bestOfBest.bestHand)) {
                    playList.add(player);
                }
            }
            int size = playList.size();
            int addChip = totalCount / size;
            for (Player player : playList) {

                player.setChip(player.getChip() + addChip);
                result.append(player.getName()).append("+").append(addChip);
                result.append("\n");
            }


        } else {
            for (Player player : listPlayerMap.keySet()) {

                if (PokerHandEvaluator.areHandsEqual(listPlayerMap.get(player), bestOfBest.bestHand)) {
                    int chip = player.getChip();
                    chip += totalCount;
                    player.setChip(chip);

                    result.append(player.getName());
                    result.append("  win chip+");
                    result.append(totalCount);
                    break;
                }
            }
        }

        sendMessageToAll(result.toString());
        showPlayList();
    }


    /**
     * 向客户端推送消息
     *
     * @param message 消息
     */
    public void sendMessage(String message) {

        if (this.session.isOpen()) {
            try {

                this.session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.error("会话已关闭，无法发送消息: " + message);
        }
        log.info("推送消息给客户端: " + this.session.getId() + "，消息内容为：" + message);
    }

    /**
     * 群发消息
     *
     * @param message 消息
     */
    public static void sendMessageToAll(String message) {
        for (WsServer wsServer : wsServers) {
            wsServer.sendMessage(message);
        }
    }


    /**
     * 连接发生报错时执行
     *
     * @param session   会话
     * @param throwable 报错
     */
    @OnError
    public void onError(Session session, @NonNull Throwable throwable) {
        log.error("连接发生报错");
        clientJetton.remove(this);
        throwable.printStackTrace();
    }

    /**
     * 连接断开时执行
     */
    @OnClose
    public void onClose() {
        //接入客户端连接数-1
        int count = onlineCount.decrementAndGet();
        //集合中的客户端对象-1
        wsServers.remove(this);
        clientJetton.remove(this);
        log.info("服务端断开连接，当前连接的客户端数量为：{}", count);
    }
}
