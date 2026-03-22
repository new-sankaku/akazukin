package com.akazukin.domain.service;

import com.akazukin.domain.model.SeasonalEvent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SeasonalEventProvider {

    public List<SeasonalEvent> getEvents(int year) {
        List<SeasonalEvent> events = new ArrayList<>();
        events.add(new SeasonalEvent(LocalDate.of(year, 1, 7), "七草", "Nanakusa",
                "七草粥で無病息災を願う。健康・食系コンテンツに最適",
                "Seven herbs porridge for health. Perfect for health and food content"));
        events.add(new SeasonalEvent(LocalDate.of(year, 2, 3), "節分", "Setsubun",
                "豆まき・恵方巻き。参加型コンテンツ企画に適している",
                "Bean throwing and Ehomaki. Great for participatory content"));
        events.add(new SeasonalEvent(LocalDate.of(year, 2, 14), "バレンタインデー", "Valentine's Day",
                "チョコレート・ギフト提案。ECとの連動投稿が効果的",
                "Chocolate and gift suggestions. EC-linked posts are effective"));
        events.add(new SeasonalEvent(LocalDate.of(year, 3, 3), "ひな祭り", "Hinamatsuri",
                "桃の節句。女の子向け・家族向けの温かい投稿が好反応",
                "Girls' festival. Family-oriented warm posts get good engagement"));
        events.add(new SeasonalEvent(LocalDate.of(year, 3, 8), "国際女性デー", "International Women's Day",
                "女性向け商品・サービスの訴求に最適。ミモザのビジュアルが定番",
                "Ideal for women-targeted products. Mimosa visuals are standard"));
        events.add(new SeasonalEvent(LocalDate.of(year, 3, 14), "ホワイトデー", "White Day",
                "ギフト提案が有効。バレンタインの返礼として男性向け訴求も",
                "Gift suggestions work well. Also targets men as Valentine's return"));
        events.add(new SeasonalEvent(LocalDate.of(year, 3, 27), "さくらの日", "Sakura Day",
                "3×9=27の語呂合わせ。桜フォトコンテスト等の参加型企画が有効",
                "Wordplay: 3x9=27. Cherry blossom photo contests work well"));
        events.add(new SeasonalEvent(LocalDate.of(year, 5, 5), "端午の節句", "Tango no Sekku",
                "こいのぼり・柏餅。家族向けコンテンツに最適",
                "Carp streamers and kashiwa-mochi. Perfect for family content"));
        events.add(new SeasonalEvent(LocalDate.of(year, 6, 16), "父の日", "Father's Day",
                "ギフト系投稿が効果的。感謝のメッセージ企画も好反応",
                "Gift posts are effective. Thank-you message campaigns resonate"));
        events.add(new SeasonalEvent(LocalDate.of(year, 7, 7), "七夕", "Tanabata",
                "願い事企画が定番。参加型コンテンツで盛り上がる",
                "Wish-making campaigns are standard. Participatory content thrives"));
        events.add(new SeasonalEvent(LocalDate.of(year, 8, 15), "お盆", "Obon",
                "帰省・家族系投稿。ノスタルジックなコンテンツが好反応",
                "Homecoming and family posts. Nostalgic content resonates well"));
        events.add(new SeasonalEvent(LocalDate.of(year, 9, 15), "十五夜", "Juugoya",
                "お月見。和菓子・秋の夜長系コンテンツが効果的",
                "Moon viewing. Japanese sweets and autumn night content works well"));
        events.add(new SeasonalEvent(LocalDate.of(year, 10, 31), "ハロウィン", "Halloween",
                "仮装・限定コンテンツ。SNS映えする投稿が高エンゲージメント",
                "Costumes and limited content. SNS-friendly posts get high engagement"));
        events.add(new SeasonalEvent(LocalDate.of(year, 11, 15), "七五三", "Shichi-Go-San",
                "子どもの成長祝い。家族向け・記念コンテンツが好反応",
                "Children's growth celebration. Family and memorial content resonates"));
        events.add(new SeasonalEvent(LocalDate.of(year, 12, 25), "クリスマス", "Christmas",
                "ギフト・イルミネーション系。12月前半から投稿準備が必要",
                "Gifts and illuminations. Start preparing posts from early December"));
        events.add(new SeasonalEvent(LocalDate.of(year, 12, 31), "大晦日", "New Year's Eve",
                "年末の振り返り・新年の抱負。感謝のメッセージが定番",
                "Year-end review and resolutions. Thank-you messages are standard"));
        return events;
    }
}
