# Serwis Informacyjny Białegostoku 🎉

RESTowy web service do zarządzania wydarzeniami kulturalnymi w Białymstoku. Zawiera klienta w Pythonie z interfejsem graficznym (Tkinter).

## Funkcje ✨
- **CRUD dla wydarzeń**: Dodawanie, edycja, usuwanie, przeglądanie szczegółów.
- **Wyszukiwanie**: Filtruj wydarzenia po dacie lub tygodniu.
- **Raporty PDF**: Generuj podsumowania wydarzeń w formacie PDF.
- **HATEOAS**: Automatyczne linki w odpowiedziach API.
- **Bezpieczeństwo**: Autentykacja BasicAuth + SSL (HTTPS).
- **Obsługa błędów**: Spersonalizowane komunikaty i kody HTTP.
- **Klient GUI**: Aplikacja desktopowa w Pythonie.
- **Dodatkowe punkty**:
  - Klient w innym języku niż serwer (Python vs Java).
  - Bezpieczeństwo przez BasicAuth + SSL.
  - Testowanie z użyciem narzędzi jak Postman.

---

## Endpointy API 🛠️

| Endpoint                 | Metoda | Opis                                      | Przykład żądania                         |
|--------------------------|--------|-------------------------------------------|------------------------------------------|
| `/events`                | GET    | Pobierz wszystkie wydarzenia              | `GET https://localhost:8181/events`      |
| `/events/{id}`           | GET    | Pobierz wydarzenie po ID                  | `GET https://localhost:8181/events/1`    |
| `/events/date/{date}`    | GET    | Wydarzenia dla daty (YYYY-MM-DD)          | `GET /events/date/2024-06-20`            |
| `/events/week`           | GET    | Wydarzenia dla tygodnia (parametry: `week`, `year`) | `GET /events/week?week=25&year=2024` |
| `/events`                | POST   | Dodaj nowe wydarzenie                     | `POST /events` z ciałem JSON             |
| `/events/{id}`           | PUT    | Zaktualizuj wydarzenie                    | `PUT /events/1` z ciałem JSON            |
| `/events/{id}`           | DELETE | Usuń wydarzenie                           | `DELETE /events/1`                       |
| `/events/report`         | GET    | Generuj raport PDF (parametry: `date`/`byWeek`) | `GET /events/report?date=2024-06-20`  |
| `/events/types`          | GET    | Lista typów wydarzeń                      | `GET /events/types`                      |

---

## Bezpieczeństwo 🔒
- **Basic Authentication**:  
  - Login: `admin` | Hasło: `admin`
  - Nagłówek: `Authorization: Basic YWRtaW46YWRtaW4=`
- **SSL**: Wymagane HTTPS (domyślnie self-signed certyfikat)
