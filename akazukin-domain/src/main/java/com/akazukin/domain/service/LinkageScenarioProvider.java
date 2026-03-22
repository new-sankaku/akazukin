package com.akazukin.domain.service;

import com.akazukin.domain.model.LinkageScenario;

import java.util.List;

public class LinkageScenarioProvider {

    public List<LinkageScenario> getScenarios() {
        return List.of(
                new LinkageScenario("長文展開型", "Long-form Expansion", List.of(
                        new LinkageScenario.LinkageStep("note", "詳細な長文記事を投稿", "Publish detailed long-form article"),
                        new LinkageScenario.LinkageStep("X (Twitter)", "要約+リンクで拡散", "Spread with summary + link"),
                        new LinkageScenario.LinkageStep("niconico", "コメント誘導で議論活性化", "Activate discussion via comments")
                )),
                new LinkageScenario("ビジュアル展開型", "Visual Expansion", List.of(
                        new LinkageScenario.LinkageStep("YouTube", "メイン動画を公開", "Publish main video"),
                        new LinkageScenario.LinkageStep("TikTok", "ショート版で新規層獲得", "Gain new audience with short version"),
                        new LinkageScenario.LinkageStep("Instagram", "ハイライト画像+ストーリーズ", "Highlight images + stories")
                )),
                new LinkageScenario("コミュニティ育成型", "Community Nurturing", List.of(
                        new LinkageScenario.LinkageStep("X (Twitter)", "アンケート+話題提起", "Survey + raise topic"),
                        new LinkageScenario.LinkageStep("note", "結果まとめ+考察記事", "Summary + analysis article"),
                        new LinkageScenario.LinkageStep("Threads", "深掘りディスカッション", "In-depth discussion")
                )),
                new LinkageScenario("EC連動型", "EC Integration", List.of(
                        new LinkageScenario.LinkageStep("Instagram", "商品ビジュアル+タグ", "Product visuals + tags"),
                        new LinkageScenario.LinkageStep("Pinterest", "カタログピンで保存促進", "Catalog pins for saves"),
                        new LinkageScenario.LinkageStep("LINE", "クーポン配信で購買誘導", "Coupon delivery for purchase")
                ))
        );
    }
}
