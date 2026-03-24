#!/usr/bin/env python3
import json

# Major cities with their counties/provinces
cities_with_counties = {
    # Turkey
    "Istanbul": "Istanbul",
    "Ankara": "Ankara",
    "Izmir": "Izmir",
    "Bursa": "Bursa",
    "Antalya": "Antalya",
    "Adana": "Adana",
    "Konya": "Konya",
    "Gaziantep": "Gaziantep",
    "Mersin": "Mersin",
    "Diyarbakir": "Diyarbakir",
    "Kayseri": "Kayseri",
    "Eskisehir": "Eskisehir",
    "Samsun": "Samsun",
    "Trabzon": "Trabzon",
    "Denizli": "Denizli",
    "Malatya": "Malatya",
    "Erzurum": "Erzurum",
    "Van": "Van",
    "Batman": "Batman",
    "Elazig": "Elazig",
    "Kahramanmaras": "Kahramanmaras",
    "Sakarya": "Sakarya",
    "Mugla": "Mugla",
    "Aydin": "Aydin",
    "Manisa": "Manisa",
    "Balikesir": "Balikesir",
    "Canakkale": "Canakkale",
    "Tekirdag": "Tekirdag",
    "Edirne": "Edirne",
    "Kirklareli": "Kirklareli",
    "Sivas": "Sivas",
    "Tokat": "Tokat",
    "Ordu": "Ordu",
    "Giresun": "Giresun",
    "Rize": "Rize",
    "Artvin": "Artvin",
    "Bolu": "Bolu",
    "Duzce": "Duzce",
    "Zonguldak": "Zonguldak",
    "Bartin": "Bartin",
    "Karabuk": "Karabuk",
    "Cankiri": "Cankiri",
    "Kastamonu": "Kastamonu",
    "Corum": "Corum",
    "Amasya": "Amasya",
    "Tokat": "Tokat",
    "Yozgat": "Yozgat",
    "Kirsehir": "Kirsehir",
    "Nevsehir": "Nevsehir",
    "Aksaray": "Aksaray",
    "Nigde": "Nigde",
    "Karaman": "Karaman",
    "Konya": "Konya",
    "Afyon": "Afyon",
    "Usak": "Usak",
    "Kutahya": "Kutahya",
    "Bilecik": "Bilecik",
    "Sakarya": "Sakarya",
    "Duzce": "Duzce",
    "Zonguldak": "Zonguldak",
    "Bartin": "Bartin",
    "Karabuk": "Karabuk",
    "Bolu": "Bolu",
    "Cankiri": "Cankiri",
    "Ankara": "Ankara",
    "Eskisehir": "Eskisehir",
    "Istanbul": "Istanbul",
    "Izmir": "Izmir",
    "Bursa": "Bursa",
    "Kocaeli": "Kocaeli",
    "Sakarya": "Sakarya",
    "Yalova": "Yalova",
    
    # Saudi Arabia
    "Mecca": "Mecca",
    "Riyadh": "Riyadh",
    "Medina": "Medina",
    "Jeddah": "Mecca",
    "Dammam": "Eastern",
    "Taif": "Mecca",
    
    # Egypt
    "Cairo": "Cairo",
    "Alexandria": "Alexandria",
    "Giza": "Giza",
    "Luxor": "Luxor",
    
    # Indonesia
    "Jakarta": "Jakarta",
    "Surabaya": "East Java",
    "Bandung": "West Java",
    "Medan": "North Sumatra",
    "Makassar": "South Sulawesi",
    
    # Malaysia
    "Kuala Lumpur": "Federal Territory",
    "Johor Bahru": "Johor",
    "Penang": "Penang",
    
    # Pakistan
    "Karachi": "Sindh",
    "Lahore": "Punjab",
    "Islamabad": "Capital",
    "Peshawar": "Khyber Pakhtunkhwa",
    "Quetta": "Baluchistan",
    
    # India
    "Mumbai": "Maharashtra",
    "Delhi": "Delhi",
    "Kolkata": "West Bengal",
    "Chennai": "Tamil Nadu",
    "Bangalore": "Karnataka",
    "Hyderabad": "Telangana",
    "Ahmedabad": "Gujarat",
    "Jaipur": "Rajasthan",
    
    # Bangladesh
    "Dhaka": "Dhaka",
    "Chittagong": "Chittagong",
    
    # Nigeria
    "Lagos": "Lagos",
    "Abuja": "Federal Capital",
    "Kano": "Kano",
    "Ibadan": "Oyo",
    
    # Morocco
    "Casablanca": "Casablanca-Settat",
    "Rabat": "Rabat-Sale-Kenitra",
    "Marrakesh": "Marrakesh-Safi",
    "Fes": "Fes-Meknes",
    
    # Algeria
    "Algiers": "Algiers",
    "Oran": "Oran",
    "Constantine": "Constantine",
    
    # Tunisia
    "Tunis": "Tunis",
    "Sfax": "Sfax",
    "Sousse": "Sousse",
    
    # Jordan
    "Amman": "Amman",
    "Zarqa": "Zarqa",
    "Irbid": "Irbid",
    
    # UAE
    "Dubai": "Dubai",
    "Abu Dhabi": "Abu Dhabi",
    "Sharjah": "Sharjah",
    "Ajman": "Ajman",
    
    # Kuwait
    "Kuwait City": "Capital",
    "Hawalli": "Hawalli",
    
    # Qatar
    "Doha": "Doha",
    
    # Bahrain
    "Manama": "Capital",
    
    # Oman
    "Muscat": "Muscat",
    "Salalah": "Dhofar",
    
    # Germany
    "Berlin": "Berlin",
    "Munich": "Bavaria",
    "Hamburg": "Hamburg",
    "Frankfurt": "Hesse",
    "Cologne": "North Rhine-Westphalia",
    
    # France
    "Paris": "Ile-de-France",
    "Marseille": "Provence-Alpes-Cote d'Azur",
    "Lyon": "Auvergne-Rhone-Alpes",
    "Nice": "Provence-Alpes-Cote d'Azur",
    
    # UK
    "London": "England",
    "Birmingham": "England",
    "Manchester": "England",
    "Edinburgh": "Scotland",
    "Glasgow": "Scotland",
    "Cardiff": "Wales",
    "Belfast": "Northern Ireland",
    
    # USA
    "New York": "New York",
    "Los Angeles": "California",
    "Chicago": "Illinois",
    "Houston": "Texas",
    "Phoenix": "Arizona",
    "Philadelphia": "Pennsylvania",
    "San Antonio": "Texas",
    "San Diego": "California",
    "Dallas": "Texas",
    "San Jose": "California",
    "Austin": "Texas",
    "Jacksonville": "Florida",
    "Fort Worth": "Texas",
    "Columbus": "Ohio",
    "Charlotte": "North Carolina",
    "Indianapolis": "Indiana",
    "Seattle": "Washington",
    "Denver": "Colorado",
    "Boston": "Massachusetts",
    "Detroit": "Michigan",
    
    # Netherlands
    "Amsterdam": "North Holland",
    "Rotterdam": "South Holland",
    "The Hague": "South Holland",
    "Utrecht": "Utrecht",
}

# Read the file
with open('/Users/a195143/BitBucket/Compose/NamazVakitleri/prayer_settings/src/main/assets/cities.json', 'r') as f:
    data = json.load(f)

# Update cities with counties
for city in data['cities']:
    name = city.get('name', '')
    county = cities_with_counties.get(name)
    if county:
        city['county'] = county

# Write the file back
with open('/Users/a195143/BitBucket/Compose/NamazVakitleri/prayer_settings/src/main/assets/cities.json', 'w') as f:
    json.dump(data, f, indent=2)

print(f"Updated {len(data['cities'])} cities")
