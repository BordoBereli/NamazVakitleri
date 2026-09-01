#!/usr/bin/env python3
"""Add cityTr/cityAr/cityFa fields for Turkey provinces in cities.json."""
import json
import os

CITIES_JSON = os.path.normpath(
    os.path.join(os.path.dirname(__file__), "..", "prayer_settings", "src", "main", "assets", "cities.json")
)

PROVINCES = {
    "Adana": {"tr": "Adana", "ar": "أضنة", "fa": "آدانا"},
    "Afyon": {"tr": "Afyonkarahisar", "ar": "أفيون قره حصار", "fa": "افیون قرهحصار"},
    "Aksaray": {"tr": "Aksaray", "ar": "أقصراي", "fa": "آقسرای"},
    "Amasya": {"tr": "Amasya", "ar": "أماسيا", "fa": "آماسیه"},
    "Ankara": {"tr": "Ankara", "ar": "أنقرة", "fa": "آنکارا"},
    "Antalya": {"tr": "Antalya", "ar": "أنطاليا", "fa": "آنتالیا"},
    "Ardahan": {"tr": "Ardahan", "ar": "أردهان", "fa": "اردهان"},
    "Artvin": {"tr": "Artvin", "ar": "أرتوين", "fa": "آرتوین"},
    "Aydin": {"tr": "Aydın", "ar": "أيدين", "fa": "آیدین"},
    "Balikesir": {"tr": "Balıkesir", "ar": "باليكسير", "fa": "بالیکسیر"},
    "Bartin": {"tr": "Bartın", "ar": "بارتين", "fa": "بارتین"},
    "Batman": {"tr": "Batman", "ar": "بطمان", "fa": "باطمان"},
    "Bayburt": {"tr": "Bayburt", "ar": "بايبورت", "fa": "بایبورت"},
    "Bilecik": {"tr": "Bilecik", "ar": "بيله جك", "fa": "بیلهجیک"},
    "Bingol": {"tr": "Bingöl", "ar": "بينغول", "fa": "بینگول"},
    "Bitlis": {"tr": "Bitlis", "ar": "بدليس", "fa": "بتلیس"},
    "Bolu": {"tr": "Bolu", "ar": "بولو", "fa": "بولو"},
    "Burdur": {"tr": "Burdur", "ar": "بوردور", "fa": "بوردور"},
    "Bursa": {"tr": "Bursa", "ar": "بورصة", "fa": "بورسا"},
    "Canakkale": {"tr": "Çanakkale", "ar": "جناق قلعة", "fa": "چاناققلعه"},
    "Cankiri": {"tr": "Çankırı", "ar": "جانقري", "fa": "چانقری"},
    "Denizli": {"tr": "Denizli", "ar": "دنيزلي", "fa": "دنیزلی"},
    "Diyarbakir": {"tr": "Diyarbakır", "ar": "ديار بكر", "fa": "دیاربکر"},
    "Duzce": {"tr": "Düzce", "ar": "دوزجه", "fa": "دوزجه"},
    "Edirne": {"tr": "Edirne", "ar": "أدرنة", "fa": "ادرنه"},
    "Elazig": {"tr": "Elazığ", "ar": "إلازيغ", "fa": "الازیغ"},
    "Erzurum": {"tr": "Erzurum", "ar": "أرضروم", "fa": "ارزروم"},
    "Eskisehir": {"tr": "Eskişehir", "ar": "إسكي شهر", "fa": "اسکیشهر"},
    "Gaziantep": {"tr": "Gaziantep", "ar": "غازي عنتاب", "fa": "غازیعینتاب"},
    "Giresun": {"tr": "Giresun", "ar": "غيرسون", "fa": "گیرسون"},
    "Gumushane": {"tr": "Gümüşhane", "ar": "كوموش خانة", "fa": "گوموشخانه"},
    "Hakkari": {"tr": "Hakkâri", "ar": "هكاري", "fa": "حکاری"},
    "Hatay": {"tr": "Hatay", "ar": "هاتاي", "fa": "هاتای"},
    "Igdir": {"tr": "Iğdır", "ar": "إغدير", "fa": "ایغدیر"},
    "Isparta": {"tr": "Isparta", "ar": "إسبرطة", "fa": "اسپارتا"},
    "Istanbul": {"tr": "İstanbul", "ar": "إسطنبول", "fa": "استانبول"},
    "Izmir": {"tr": "İzmir", "ar": "إزمير", "fa": "ازمیر"},
    "Kahramanmaras": {"tr": "Kahramanmaraş", "ar": "قهرمان مرعش", "fa": "قهرمانمرعش"},
    "Karabuk": {"tr": "Karabük", "ar": "كارابوك", "fa": "کارابوک"},
    "Karaman": {"tr": "Karaman", "ar": "قرة مان", "fa": "کارامان"},
    "Kastamonu": {"tr": "Kastamonu", "ar": "قسطموني", "fa": "کاستامونو"},
    "Kayseri": {"tr": "Kayseri", "ar": "قيصرية", "fa": "قیصریه"},
    "Kilis": {"tr": "Kilis", "ar": "كلس", "fa": "کیلیس"},
    "Kirikkale": {"tr": "Kırıkkale", "ar": "قيريق قلعة", "fa": "قیریققلعه"},
    "Kirklareli": {"tr": "Kırklareli", "ar": "قرقلر ايلي", "fa": "قرقلرایلی"},
    "Kirsehir": {"tr": "Kırşehir", "ar": "قير شهير", "fa": "قرشهر"},
    "Kocaeli": {"tr": "Kocaeli", "ar": "قوجه ايلي", "fa": "قوجاایلی"},
    "Konya": {"tr": "Konya", "ar": "قونية", "fa": "قونیه"},
    "Kutahya": {"tr": "Kütahya", "ar": "كوتاهية", "fa": "کوتاهیه"},
    "Malatya": {"tr": "Malatya", "ar": "ملطية", "fa": "ملطیه"},
    "Manisa": {"tr": "Manisa", "ar": "مانيسا", "fa": "مانیسا"},
    "Mardin": {"tr": "Mardin", "ar": "ماردين", "fa": "ماردین"},
    "Mersin": {"tr": "Mersin", "ar": "مرسين", "fa": "مرسین"},
    "Mus": {"tr": "Muş", "ar": "موش", "fa": "موش"},
    "Nevsehir": {"tr": "Nevşehir", "ar": "نوشهر", "fa": "نوشهر"},
    "Nigde": {"tr": "Niğde", "ar": "نيدا", "fa": "نیغده"},
    "Ordu": {"tr": "Ordu", "ar": "أوردو", "fa": "اردو"},
    "Rize": {"tr": "Rize", "ar": "ريزه", "fa": "ریزه"},
    "Sakarya": {"tr": "Sakarya", "ar": "سكاريا", "fa": "ساکاریا"},
    "Samsun": {"tr": "Samsun", "ar": "سامسون", "fa": "سامسون"},
    "Sanliurfa": {"tr": "Şanlıurfa", "ar": "شانلي أورفة", "fa": "شانلیاورفا"},
    "Siirt": {"tr": "Siirt", "ar": "سعرد", "fa": "سعرد"},
    "Sinop": {"tr": "Sinop", "ar": "سينوب", "fa": "سینوپ"},
    "Sirnak": {"tr": "Şırnak", "ar": "شرناق", "fa": "شرناق"},
    "Sivas": {"tr": "Sivas", "ar": "سيواس", "fa": "سیواس"},
    "Tekirdag": {"tr": "Tekirdağ", "ar": "تكيرداغ", "fa": "تکیرداغ"},
    "Tokat": {"tr": "Tokat", "ar": "توقات", "fa": "توقات"},
    "Trabzon": {"tr": "Trabzon", "ar": "طرابزون", "fa": "ترابزون"},
    "Tunceli": {"tr": "Tunceli", "ar": "تونجلي", "fa": "تونجلی"},
    "Usak": {"tr": "Uşak", "ar": "أوشاك", "fa": "اوشاک"},
    "Van": {"tr": "Van", "ar": "وان", "fa": "وان"},
    "Yalova": {"tr": "Yalova", "ar": "يالوفا", "fa": "یالووا"},
    "Yozgat": {"tr": "Yozgat", "ar": "يوزغات", "fa": "یوزگات"},
    "Zonguldak": {"tr": "Zonguldak", "ar": "زونغولداك", "fa": "زونگولداک"},
}

def main():
    with open(CITIES_JSON, encoding="utf-8") as f:
        data = json.load(f)
    updated = 0
    for city in data["cities"]:
        prov = city.get("city")
        if prov and prov in PROVINCES:
            for lang, value in PROVINCES[prov].items():
                city[f"city{lang.capitalize()}"] = value
            updated += 1
    with open(CITIES_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"Localized province names for {updated} cities")

if __name__ == "__main__":
    main()
