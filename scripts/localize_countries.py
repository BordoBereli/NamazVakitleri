#!/usr/bin/env python3
"""Add countryTr/countryAr/countryFa fields to every city in cities.json."""
import json
import os

CITIES_JSON = os.path.normpath(
    os.path.join(os.path.dirname(__file__), "..", "prayer_settings", "src", "main", "assets", "cities.json")
)

COUNTRIES = {
    "Turkey": {"tr": "Türkiye", "ar": "تركيا", "fa": "ترکیه"},
    "Saudi Arabia": {"tr": "Suudi Arabistan", "ar": "السعودية", "fa": "عربستان سعودی"},
    "Egypt": {"tr": "Mısır", "ar": "مصر", "fa": "مصر"},
    "Indonesia": {"tr": "Endonezya", "ar": "إندونيسيا", "fa": "اندونزی"},
    "Pakistan": {"tr": "Pakistan", "ar": "باكستان", "fa": "پاکستان"},
    "Iran": {"tr": "İran", "ar": "إيران", "fa": "ایران"},
    "China": {"tr": "Çin", "ar": "الصين", "fa": "چین"},
    "Algeria": {"tr": "Cezayir", "ar": "الجزائر", "fa": "الجزایر"},
    "Malaysia": {"tr": "Malezya", "ar": "ماليزيا", "fa": "مالزی"},
    "India": {"tr": "Hindistan", "ar": "الهند", "fa": "هند"},
    "Morocco": {"tr": "Fas", "ar": "المغرب", "fa": "مراکش"},
    "Iraq": {"tr": "Irak", "ar": "العراق", "fa": "عراق"},
    "Russia": {"tr": "Rusya", "ar": "روسيا", "fa": "روسیه"},
    "Syria": {"tr": "Suriye", "ar": "سوريا", "fa": "سوریه"},
    "USA": {"tr": "ABD", "ar": "الولايات المتحدة", "fa": "ایالات متحده آمریکا"},
    "Tunisia": {"tr": "Tunus", "ar": "تونس", "fa": "تونس"},
    "Jordan": {"tr": "Ürdün", "ar": "الأردن", "fa": "اردن"},
    "UAE": {"tr": "BAE", "ar": "الإمارات", "fa": "امارات متحده عربی"},
    "Australia": {"tr": "Avustralya", "ar": "أستراليا", "fa": "استرالیا"},
    "Germany": {"tr": "Almanya", "ar": "ألمانيا", "fa": "آلمان"},
    "Bahrain": {"tr": "Bahreyn", "ar": "البحرين", "fa": "بحرین"},
    "Oman": {"tr": "Umman", "ar": "عمان", "fa": "عمان"},
    "Canada": {"tr": "Kanada", "ar": "كندا", "fa": "کانادا"},
    "France": {"tr": "Fransa", "ar": "فرنسا", "fa": "فرانسه"},
    "Japan": {"tr": "Japonya", "ar": "اليابان", "fa": "ژاپن"},
    "Lebanon": {"tr": "Lübnan", "ar": "لبنان", "fa": "لبنان"},
    "South Africa": {"tr": "Güney Afrika", "ar": "جنوب أفريقيا", "fa": "آفریقای جنوبی"},
    "United Kingdom": {"tr": "Birleşik Krallık", "ar": "المملكة المتحدة", "fa": "بریتانیا"},
    "Bangladesh": {"tr": "Bangladeş", "ar": "بنغلاديش", "fa": "بنگلادش"},
    "Nigeria": {"tr": "Nijerya", "ar": "نيجيريا", "fa": "نیجریه"},
    "Kuwait": {"tr": "Kuveyt", "ar": "الكويت", "fa": "کویت"},
    "Qatar": {"tr": "Katar", "ar": "قطر", "fa": "قطر"},
    "Greece": {"tr": "Yunanistan", "ar": "اليونان", "fa": "یونان"},
    "Italy": {"tr": "İtalya", "ar": "إيطاليا", "fa": "ایتالیا"},
    "Libya": {"tr": "Libya", "ar": "ليبيا", "fa": "لیبی"},
    "Netherlands": {"tr": "Hollanda", "ar": "هولندا", "fa": "هلند"},
    "Poland": {"tr": "Polonya", "ar": "بولندا", "fa": "لهستان"},
    "Portugal": {"tr": "Portekiz", "ar": "البرتغال", "fa": "پرتغال"},
    "South Korea": {"tr": "Güney Kore", "ar": "كوريا الجنوبية", "fa": "کره جنوبی"},
    "Spain": {"tr": "İspanya", "ar": "إسبانيا", "fa": "اسپانیا"},
    "Switzerland": {"tr": "İsviçre", "ar": "سويسرا", "fa": "سوئیس"},
    "Vietnam": {"tr": "Vietnam", "ar": "فيتنام", "fa": "ویتنام"},
    "Austria": {"tr": "Avusturya", "ar": "النمسا", "fa": "اتریش"},
    "Belgium": {"tr": "Belçika", "ar": "بلجيكا", "fa": "بلژیک"},
    "Cambodia": {"tr": "Kamboçya", "ar": "كمبوديا", "fa": "کامبوج"},
    "Czech Republic": {"tr": "Çek Cumhuriyeti", "ar": "التشيك", "fa": "جمهوری چک"},
    "Denmark": {"tr": "Danimarka", "ar": "الدنمارك", "fa": "دانمارک"},
    "Ethiopia": {"tr": "Etiyopya", "ar": "إثيوبيا", "fa": "اتیوپی"},
    "Finland": {"tr": "Finlandiya", "ar": "فنلندا", "fa": "فنلاند"},
    "Ghana": {"tr": "Gana", "ar": "غانا", "fa": "غنا"},
    "Hong Kong": {"tr": "Hong Kong", "ar": "هونغ كونغ", "fa": "هنگ کنگ"},
    "Hungary": {"tr": "Macaristan", "ar": "المجر", "fa": "مجارستان"},
    "Ireland": {"tr": "İrlanda", "ar": "أيرلندا", "fa": "ایرلند"},
    "Kenya": {"tr": "Kenya", "ar": "كينيا", "fa": "کنیا"},
    "Madagascar": {"tr": "Madagaskar", "ar": "مدغشقر", "fa": "ماداگاسکار"},
    "Nepal": {"tr": "Nepal", "ar": "نيبال", "fa": "نپال"},
    "Norway": {"tr": "Norveç", "ar": "النرويج", "fa": "نروژ"},
    "Philippines": {"tr": "Filipinler", "ar": "الفلبين", "fa": "فیلیپین"},
    "Romania": {"tr": "Romanya", "ar": "رومانيا", "fa": "رومانی"},
    "Singapore": {"tr": "Singapur", "ar": "سنغافورة", "fa": "سنگاپور"},
    "Somalia": {"tr": "Somali", "ar": "الصومال", "fa": "سومالی"},
    "Sri Lanka": {"tr": "Sri Lanka", "ar": "سريلانكا", "fa": "سریلانکا"},
    "Sudan": {"tr": "Sudan", "ar": "السودان", "fa": "سودان"},
    "Sweden": {"tr": "İsveç", "ar": "السويد", "fa": "سوئد"},
    "Taiwan": {"tr": "Tayvan", "ar": "تايوان", "fa": "تایوان"},
    "Tanzania": {"tr": "Tanzanya", "ar": "تنزانيا", "fa": "تانزانیا"},
    "Thailand": {"tr": "Tayland", "ar": "تايلاند", "fa": "تایلند"},
    "Uganda": {"tr": "Uganda", "ar": "أوغندا", "fa": "اوگاندا"},
}

def main():
    with open(CITIES_JSON, encoding="utf-8") as f:
        data = json.load(f)
    updated = 0
    for city in data["cities"]:
        country = city["country"]
        if country in COUNTRIES:
            for lang, value in COUNTRIES[country].items():
                city[f"country{lang.capitalize()}"] = value
            updated += 1
    with open(CITIES_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"Localized country names for {updated} cities")

if __name__ == "__main__":
    main()
