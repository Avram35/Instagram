# Instagram 

Пројектни задатак из предмета **Пројектовање информационих система и база података**
Факултет инжењерских наука, Универзитет у Крагујевцу - 2025/2026

---

## Чланови тима

| Име и презиме | GitHub | Улога | Сервиси |
|---|---|---|---|
| Михајло Тимотијевић | [@mihajlotimo](https://github.com/mihajlotimo) | Frontend Engineer | `frontend` |
| Милан Аврамовић | [@avram35](https://github.com/avram35) | Backend Engineer A (DevOps) | `auth-service`, `user-service` |
| Јелена Димитријевић | [@itjelenadimitrijevic](https://github.com/itjelenadimitrijevic) | Backend Engineer B (API тестови) | `interactive-service`, `post-service` |
| Мина Ристић | [@notminaristic](https://github.com/notminaristic) | Backend Engineer C (UI тестови) | `follow-service`, `blok-service`, `feed-service` |

---

## О пројекту

Апликација представља реплику друштвене мреже Instagram, имплементирана као скуп микросервиса. Подржава регистрацију и аутентификацију корисника, праћење профила, објављивање фотографија и видео садржаја, лајковање, коментарисање, блокирање, претрагу профила и временску линију.

---

## Архитектура

Апликација је имплементирана као скуп од **7 микросервиса**. Сваки сервис, осим `feed-service`-а, има своју PostgreSQL базу података. Frontend је React + Vite апликација која директно комуницира са сервисима. Безбедност је обезбеђена путем JWT токена, док интерна комуникација између сервиса користи заштићене интерне **endpointe** са тајним кључем (`Internal-Api-Key`).

### Опис сервиса

| Сервис | Порт | Одговорности |
|---|---|---|
| `auth-service` | `8081` | Регистрација, пријава, издавање и валидација JWT токена, брисање налога |
| `user-service` | `8082` | Управљање корисничким профилима (јавни/приватни, слика, опис, претрага) |
| `follow-service` | `8083` | Праћење профила, захтеви за праћење приватних профила, уклањање пратилаца, нотификације |
| `blok-service` | `8084` | Блокирање корисника - спречава претрагу, праћење и приказ садржаја блокираног профила |
| `feed-service` | `8085` | Временска линија - хронолошки сортирана листа објава профила које корисник прати |
| `post-service` | `8086` | Креирање, уређивање и брисање објава (слике/видео, макс. 20 по објави, макс. 50 MB по фајлу) |
| `interactive-service` | `8087` | Лајковање објава, коментарисање, измена и брисање коментара |

### Безбедност

- **JWT аутентификација** - при пријави, `auth-service` генерише JWT токен који клијент чува и шаље у `Authorization: Bearer <token>` уз сваки захтев ка сервисима
- **Интерни ендпоинти** - комуникација између сервиса одвија се путем заштићених интерних **endpointa** који нису изложени кориснику; захтевају исправан `X-Internal-Api-Key` **header**

### База података

Подаци се трајно чувају кроз Docker volumes, независно по сервису. Сваки сервис има своју PostgreSQL базу, осим `feed-service`-а.

---

## Покретање апликације

### Предуслови

- [Docker](https://www.docker.com/)
- Git

### Кораци

**1. Клонирати репозиторијум**

```bash
git clone https://github.com/Avram35/Instagram.git
cd Instagram
```

**2. Подесити environment варијабле**

Linux:
```bash
cp .env.example .env
```
Windows (Command Prompt):
```cmd
copy .env.example .env
```

Кључне ствари које треба променити у `.env` за продукцију су подаци о бази и:

```env
JWT_SECRET=super-secret-jwt-key-change-this-in-production
INTERNAL_API_KEY=my-internal-service-key-instagram-2025

CORS_ALLOWED_ORIGINS=http://localhost:5173
VITE_CORS_ALLOWED_ORIGINS=http://localhost:8081

```

**3. Покренути све сервисе**

```bash
docker compose up --build
```

**4. Приступити апликацији**

| Компонента | URL |
|---|---|
| Frontend | http://localhost:5173 |

**5. Зауставити апликацију**

```bash
# Заустављање
docker compose stop

# Заустављање и брисање база (volumes)
docker compose down -v
```

---

## API Ендпоинти

Легенда: `[J]` јавни ендпоинт (без токена) | `[A]` захтева JWT токен | `[I]` интерни ендпоинт

---

### auth-service - :8081

Базна путања: `/api/v1/auth`

| Метода | Ендпоинт | Опис | Приступ |
|---|---|---|---|
| `POST` | `/api/v1/auth/signin` | Пријава корисника, враћа JWT токен | [J] |
| `POST` | `/api/v1/auth/signup` | Регистрација новог корисника | [J] |
| `DELETE` | `/api/v1/auth/delete` | Брисање налога (брише податке у свим сервисима) | [A] |
| `PUT` | `/api/v1/auth/internal/update-username` | Ажурирање корисничког имена у auth бази | [I] |

---

### user-service - :8082

Базна путања: `/api/v1/user`

| Метода | Ендпоинт | Опис | Приступ |
|---|---|---|---|
| `GET` | `/api/v1/user/{username}` | Преглед профила по корисничком имену | [A] |
| `GET` | `/api/v1/user/id/{id}` | Преглед профила по ID-у | [A] |
| `GET` | `/api/v1/user/search?query=` | Претрага корисника по имену или корисничком имену | [A] |
| `PUT` | `/api/v1/user/{id}` | Ажурирање сопственог профила | [A] |
| `POST` | `/api/v1/user/profile-pic` | Upload профилне слике (multipart) | [A] |
| `POST` | `/internal/api/v1/user` | Креирање корисничког профила при регистрацији | [I] |
| `DELETE` | `/internal/api/v1/user/{id}` | Брисање профила при брисању налога | [I] |

---

### follow-service - :8083

Базна путања: `/api/v1/follow`

| Метода | Ендпоинт | Опис | Приступ |
|---|---|---|---|
| `POST` | `/api/v1/follow/{userId}` | Слање захтева за праћење | [A] |
| `DELETE` | `/api/v1/follow/{userId}` | Престанак праћења | [A] |
| `DELETE` | `/api/v1/follow/remove/{followerId}` | Уклањање пратиоца са сопственог профила | [A] |
| `GET` | `/api/v1/follow/{userId}/followers` | Списак пратилаца профила | [A] |
| `GET` | `/api/v1/follow/{userId}/following` | Списак профила које дати профил прати | [A] |
| `GET` | `/api/v1/follow/{userId}/count` | Број пратилаца и праћених | [A] |
| `GET` | `/api/v1/follow/status/{userId}` | Статус праћења између тренутног и датог корисника | [A] |
| `GET` | `/api/v1/follow/check/{userId}` | Да ли тренутни корисник прати датог корисника | [A] |
| `POST` | `/api/v1/follow/requests/{requestId}/accept` | Прихватање захтева за праћење | [A] |
| `POST` | `/api/v1/follow/requests/{requestId}/reject` | Одбијање захтева за праћење | [A] |
| `GET` | `/api/v1/follow/requests/pending` | Листа пристиглих захтева за праћење | [A] |
| `GET` | `/api/v1/follow/requests/check/{userId}` | Да ли постоји активан захтев за датог корисника | [A] |
| `GET` | `/api/v1/follow/notifications` | Сва обавештења корисника | [A] |
| `DELETE` | `/api/v1/follow/internal/unfollow?followerId=&followingId=` | Интерно отпраћавање (нпр при блокирању) | [I] |
| `POST` | `/api/v1/follow/notifications/internal` | Креирање нотификације (лајк, коментар) | [I] |
| `POST` | `/api/v1/follow/requests/accept-all/{userId}` | Прихватање свих захтева при преласку на јавни профил | [I] |
| `GET` | `/api/v1/follow/check-internal/{followerId}/{followingId}` | Провера релације праћења | [I] |
| `DELETE` | `/api/v1/follow/internal/user/{userId}` | Брисање свих follow релација корисника | [I] |

---

### blok-service - :8084

Базна путања: `/api/v1/block`

| Метода | Ендпоинт | Опис | Приступ |
|---|---|---|---|
| `POST` | `/api/v1/block/{userId}` | Блокирање корисника | [A] |
| `DELETE` | `/api/v1/block/{userId}` | Одблокирање корисника | [A] |
| `GET` | `/api/v1/block/check/{userId}` | Да ли тренутни корисник блокира датог корисника | [A] |
| `GET` | `/api/v1/block/check-by/{userId}` | Да ли је тренутни корисник блокиран од стране датог корисника | [A] |
| `GET` | `/api/v1/block/check-either/{userId1}/{userId2}` | Провера блокаде у оба смера (позивају остали сервиси) | [I] |
| `DELETE` | `/api/v1/block/internal/user/{userId}` | Брисање свих блокова корисника при брисању налога | [I] |

---

### feed-service - :8085

Базна путања: `/api/v1/feed`

| Метода | Ендпоинт | Опис | Приступ |
|---|---|---|---|
| `GET` | `/api/v1/feed?page=0&size=20` | Временска линија (хронолошки, опадајуће), са пагинацијом | [A] |

---

### post-service - :8086

Базна путања: `/api/v1/post`

| Метода | Ендпоинт | Опис | Приступ |
|---|---|---|---|
| `POST` | `/api/v1/post` | Креирање нове објаве (макс. 20 фајлова) | [A] |
| `PUT` | `/api/v1/post/{postId}` | Измена описа објаве | [A] |
| `DELETE` | `/api/v1/post/{postId}` | Брисање целе објаве | [A] |
| `DELETE` | `/api/v1/post/{postId}/media/{mediaId}` | Уклањање појединачног медија из колажа | [A] |
| `GET` | `/api/v1/post/{postId}` | Преглед појединачне објаве | [A] |
| `GET` | `/api/v1/post/user/{userId}` | Све објаве корисника (галерија профила) | [A] |
| `GET` | `/api/v1/post/count/{userId}` | Број објава корисника | [A] |
| `GET` | `/api/v1/post/internal/{postId}` | Преузимање објаве без провере видљивости | [I] |
| `GET` | `/api/v1/post/internal/user/{userId}` | Све објаве корисника без провере видљивости | [I] |
| `DELETE` | `/api/v1/post/internal/user/{userId}` | Брисање свих објава корисника при брисању налога | [I] |

---

### interactive-service - :8087

Базна путања: `/api/v1/like` и `/api/v1/comment`

| Метода | Ендпоинт | Опис | Приступ |
|---|---|---|---|
| `POST` | `/api/v1/like/{postId}` | Лајковање објаве | [A] |
| `DELETE` | `/api/v1/like/{postId}` | Уклањање лајка | [A] |
| `GET` | `/api/v1/like/check/{postId}` | Да ли је тренутни корисник лајковао објаву | [A] |
| `GET` | `/api/v1/like/count/{postId}` | Број лајкова на објави | [A] |
| `GET` | `/api/v1/like/{postId}/list` | Списак корисника који су лајковали | [A] |
| `DELETE` | `/api/v1/like/internal/post/{postId}` | Брисање свих лајкова на објави при брисању објаве | [I] |
| `POST` | `/api/v1/comment/{postId}` | Коментарисање објаве | [A] |
| `PUT` | `/api/v1/comment/{commentId}` | Измена сопственог коментара | [A] |
| `DELETE` | `/api/v1/comment/{commentId}` | Брисање сопственог коментара | [A] |
| `GET` | `/api/v1/comment/{postId}/list` | Списак коментара на објави | [A] |
| `GET` | `/api/v1/comment/count/{postId}` | Број коментара на објави | [A] |
| `DELETE` | `/api/v1/comment/internal/post/{postId}` | Брисање свих коментара на објави при брисању објаве | [I] |

---

## CI/CD

Аутоматизовани токови су имплементирани путем **GitHub Actions**.

---

## Тестирање

Пројекат садржи три нивоа тестова: **unit тестове**, **API интеграционе тестове** и **UI интеграционе тестове**.

### Unit тестови (Backend)

Покретање unit тестова за конкретан backend сервис:

```bash
cd services/<ime-servisa>
mvn test
```

На пример:

```bash
cd services/auth-service
mvn test
```

### Unit тестови (Frontend)

Frontend unit тестови су писани у **Vitest** + **React Testing Library** и покривају следеће компоненте и странице:

| Компонента / Страница | Тест фајл |
|---|---|
| `Navbar` | `src/components/Navbar/Navbar.test.jsx` |
| `Notification` | `src/components/Notification/Notification.test.jsx` |
| `CreatePost` | `src/components/CreatePost/CreatePost.test.jsx` |
| `FollowerRow` | `src/components/FollowerRow/FollowerRow.test.jsx` |
| `FollowersModal` | `src/components/FollowersModal/FollowersModal.test.jsx` |
| `MorePanel` | `src/components/MorePanel/MorePanel.test.jsx` |
| `Search` | `src/components/Search/Search.test.jsx` |
| `SinglePost` | `src/components/SinglePost/SinglePost.test.jsx` |
| `Login` | `src/pages/Login/Login.test.jsx` |
| `EditProfile` | `src/pages/EditProfile/EditProfile.test.jsx` |
| `Profile` | `src/pages/Profile/Profile.test.jsx` |

Покретање:

```bash
cd frontend

# Покретање свих тестова
npm test

# Покретање са извештајем о покривености кода
npm run test:coverage
```

### API интеграциони тестови

API интеграциони тестови се налазе у фолдеру `api-tests/` и писани су користећи **JUnit 5** и **REST Assured**. Тестови комуницирају са покренутим Docker контејнерима и проверавају исправност свих REST ендпоинта.

#### Предуслови

- Покренути сервисе (`docker compose up -d`)

#### Покретање

```bash
cd api-tests

# Сви тестови
mvn test
```

| Тест класа | Сервис | Број тестова | Шта покрива |
|---|---|---|---|
| `AuthApiTest` | Auth (8081) | 9 | Регистрација, пријава, дупликати, валидација, аутентификација |
| `UserApiTest` | User (8082) | 12 | CRUD профила, претрага, приватност, ауторизација |
| `FollowApiTest` | Follow (8083) | 16 | Follow/unfollow, приватни профили, захтеви, нотификације |
| `BlockApiTest` | Block (8084) | 10 | Блокирање, одблокирање, интерни ендпоинти, верификација |
| `PostApiTest` | Post (8086) | 11 | Креирање, ажурирање, multipart upload, приватност објава |
| `InteractiveApiTest` | Interactive (8087) | 17 | Лајкови (CRUD), коментари (CRUD), ауторизација |
| `FeedApiTest` | Feed (8085) | 6 | Feed, пагинација, хронолошки сорт |
| **Укупно** | | **81** | |

#### Покривени сценарији

- Позитивни сценарији (успешне операције)
- Негативни сценарији (дупликати, неисправни подаци, непостојећи ресурси)
- Аутентификација (одбијање захтева без JWT токена — 401)
- Ауторизација (забрана измене туђег профила/објаве — 403)
- Приватност (приступ објавама приватног профила без праћења — 403)
- Интерни ендпоинти (провера `X-Internal-Api-Key`)

### UI интеграциони тестови
 
UI интеграциони тестови се налазе у фолдеру `ui-tests/` и писани су у **Java 21** користећи **Selenium WebDriver**, **JUnit 5** и **WebDriverManager**. Тестови покрећу Chrome browser и симулирају корисничке акције кроз апликацију (клик, унос текста, навигација, upload фајлова).
 
#### Предуслови
 
- Java 21+
- Maven 3.8+
- Google Chrome (инсталиран локално)
- Покренути сервисе (`docker compose up -d`)
- Frontend доступан на `http://localhost:5173`
 
#### Покретање
 
```bash
cd ui-tests
 
# Сви тестови (визуелно — отвара Chrome прозор)
mvn test
```
 
| Тест класа | Област | Број тестова | Шта покрива |
|---|---|---|---|
| `AuthUITest` | Аутентификација | 15 | Приказ форме, пријава (username/email), погрешна лозинка, празна поља, регистрација (успешна, дупликат, кратка лозинка, неподударање лозинки), toggle login/register, приказ/скривање лозинке, заштита рута, одјава |
| `FeedUITest` | Временска линија | 8 | Приказ објава, опис објаве, празан feed, скрол, navbar елементи (search, more, profile pic) |
| `PostUITest` | Објаве | 10 | Отварање CreatePost, select files дугме, upload слике, објављивање, опис објаве, коментар, приватни профил, брисање објаве|
| `ProfileUITest` | Профил | 13 | Приказ username-а, статистике, edit дугме, follow дугме, праћење, приватни профил, мени за блокирање, edit профила (bio, кратко име, предугачак био) |
| `SocialUITest` | Социјалне функције | 13 | Претрага (отварање, унос, резултати, клик, брисање), праћење јавног/приватног корисника, отпраћивање, блокирање, нотификације, MorePanel |
| **Укупно** | | **59** | |
 
## Побољшања
 
- Додавање **API Gateway-a** (Spring Cloud Gateway) за централизовану контролу приступа
- Додавање **lazy loading-a** за слике на frontend-у
- Имплементација **responsive дизајна** за мобилне уређаје
- Кеширање (Redis) за feed и профиле
- Увођење **HTTPS** за сву комуникацију
- Имплементација **rate limiting-а** на нивоу gateway-а
