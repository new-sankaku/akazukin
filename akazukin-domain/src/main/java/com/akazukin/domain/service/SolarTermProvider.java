package com.akazukin.domain.service;

import com.akazukin.domain.model.SolarTerm;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SolarTermProvider {

    public List<SolarTerm> getSolarTerms(int year) {
        List<SolarTerm> terms = new ArrayList<>();
        terms.add(new SolarTerm(LocalDate.of(year, 1, 5), "小寒", "Shoukan",
                "寒さが厳しくなり始める時期。冬の暮らし系コンテンツが好反応",
                "Cold intensifies. Winter lifestyle content resonates well"));
        terms.add(new SolarTerm(LocalDate.of(year, 1, 20), "大寒", "Daikan",
                "一年で最も寒い時期。温まる食事・室内コンテンツが有効",
                "Coldest time of year. Warm meals and indoor content work well"));
        terms.add(new SolarTerm(LocalDate.of(year, 2, 4), "立春", "Risshun",
                "暦の上では春の始まり。春の兆しをテーマにした投稿が反応良好",
                "Spring begins on the calendar. Posts about signs of spring perform well"));
        terms.add(new SolarTerm(LocalDate.of(year, 2, 19), "雨水", "Usui",
                "雪が雨に変わる時期。春の準備系コンテンツに適している",
                "Snow turns to rain. Suitable for spring preparation content"));
        terms.add(new SolarTerm(LocalDate.of(year, 3, 5), "啓蟄", "Keichitsu",
                "虫が動き出す季節。春の訪れを感じる自然系コンテンツが好反応",
                "Insects awaken. Nature-themed spring content gets good engagement"));
        terms.add(new SolarTerm(LocalDate.of(year, 3, 20), "春分", "Shunbun",
                "昼夜の長さが等しい日。新しいスタートをテーマにした投稿が反応良好",
                "Day and night equal. Posts themed on new beginnings perform well"));
        terms.add(new SolarTerm(LocalDate.of(year, 4, 4), "清明", "Seimei",
                "万物が清らかで明るい時期。新年度の清々しさを投稿テーマに",
                "Everything is fresh and bright. Fresh new fiscal year content"));
        terms.add(new SolarTerm(LocalDate.of(year, 4, 20), "穀雨", "Kokuu",
                "春の雨が穀物を育てる時期。恵みの雨をテーマにしたコンテンツ",
                "Spring rain nurtures crops. Content themed on blessings of rain"));
        terms.add(new SolarTerm(LocalDate.of(year, 5, 5), "立夏", "Rikka",
                "暦の上では夏の始まり。初夏のレジャー系投稿が効果的",
                "Summer begins. Early summer leisure posts are effective"));
        terms.add(new SolarTerm(LocalDate.of(year, 5, 21), "小満", "Shouman",
                "植物が成長する時期。ガーデニング・食の投稿が好反応",
                "Plants grow. Gardening and food posts get good engagement"));
        terms.add(new SolarTerm(LocalDate.of(year, 6, 5), "芒種", "Boushu",
                "田植えの時期。日本の原風景系コンテンツに適している",
                "Rice planting season. Japanese rural landscape content fits well"));
        terms.add(new SolarTerm(LocalDate.of(year, 6, 21), "夏至", "Geshi",
                "一年で最も昼が長い日。夏の到来をテーマにした投稿",
                "Longest day of the year. Posts themed on summer arrival"));
        terms.add(new SolarTerm(LocalDate.of(year, 7, 7), "小暑", "Shousho",
                "暑さが本格化する時期。夏バテ対策・涼感コンテンツが有効",
                "Heat intensifies. Summer fatigue tips and cooling content work well"));
        terms.add(new SolarTerm(LocalDate.of(year, 7, 22), "大暑", "Taisho",
                "一年で最も暑い時期。避暑・夏祭り系投稿が好反応",
                "Hottest time of year. Cooling off and festival posts resonate"));
        terms.add(new SolarTerm(LocalDate.of(year, 8, 7), "立秋", "Risshuu",
                "暦の上では秋の始まり。残暑見舞い・秋の気配を投稿テーマに",
                "Autumn begins on the calendar. Late summer greetings and autumn hints"));
        terms.add(new SolarTerm(LocalDate.of(year, 8, 23), "処暑", "Shosho",
                "暑さが峠を越える時期。秋の準備系コンテンツに適している",
                "Heat peaks and fades. Autumn preparation content fits well"));
        terms.add(new SolarTerm(LocalDate.of(year, 9, 7), "白露", "Hakuro",
                "朝露が白くなる時期。秋の自然美をテーマにした投稿",
                "Morning dew turns white. Posts themed on autumn natural beauty"));
        terms.add(new SolarTerm(LocalDate.of(year, 9, 22), "秋分", "Shuubun",
                "昼夜の長さが等しい日。お彼岸・秋の味覚系コンテンツが有効",
                "Day and night equal. Equinox and autumn food content works well"));
        terms.add(new SolarTerm(LocalDate.of(year, 10, 8), "寒露", "Kanro",
                "冷たい露が降りる時期。紅葉・秋深まるコンテンツが好反応",
                "Cold dew falls. Autumn foliage and deepening autumn content resonates"));
        terms.add(new SolarTerm(LocalDate.of(year, 10, 23), "霜降", "Soukou",
                "霜が降り始める時期。冬支度系コンテンツに適している",
                "Frost begins. Winter preparation content fits well"));
        terms.add(new SolarTerm(LocalDate.of(year, 11, 7), "立冬", "Rittou",
                "暦の上では冬の始まり。鍋・温泉系投稿が効果的",
                "Winter begins. Hot pot and onsen posts are effective"));
        terms.add(new SolarTerm(LocalDate.of(year, 11, 22), "小雪", "Shousetsu",
                "雪がちらつく時期。冬の暮らし系コンテンツが好反応",
                "Light snow begins. Winter lifestyle content gets good engagement"));
        terms.add(new SolarTerm(LocalDate.of(year, 12, 7), "大雪", "Taisetsu",
                "雪が本格化する時期。年末準備・冬の風物詩投稿が有効",
                "Heavy snow season. Year-end prep and winter tradition posts work well"));
        terms.add(new SolarTerm(LocalDate.of(year, 12, 22), "冬至", "Touji",
                "一年で最も夜が長い日。ゆず湯・かぼちゃ等の風習投稿",
                "Longest night. Yuzu bath and pumpkin tradition posts"));
        return terms;
    }
}
