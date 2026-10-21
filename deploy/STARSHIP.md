# Сборка и развёртывание Starship

Starship — второй сервис лабораторной работы. Он использует Spring MVC 5,
собирается в WAR для WildFly 21 (Jakarta EE 8 / `javax.servlet`),
использует отдельную PostgreSQL-базу и обращается к SpaceMarine по HTTPS.

## 1. Сборка

Нужен JDK 17 или новее. Из корня репозитория:

```bash
bash gradlew :starship-service:test :starship-service:war
```

Gradle сначала генерирует интерфейс сервера `StarshipApi`, REST-клиент
`SpaceMarinesApi` на `RestTemplate` и DTO из `docs/openapi.yaml`. Исходники находятся в
`starship-service/build/generated/openapi/` и попадают в WAR автоматически.
Не редактируйте их вручную: при следующей сборке они будут перезаписаны.

Для клиента используется `starship-service/openapi-templates/enumOuterClass.mustache`.
Если SpaceMarine вернёт неизвестный вариант `Weapon`, десериализация завершится
ошибкой вместо того, чтобы незаметно превратить значение в `null`.

Итоговый файл: `starship-service/build/libs/starship-service.war`.

## 2. База данных

Создайте отдельную базу/пользователя PostgreSQL для второго сервиса и выполните
`db/starship.sql` в этой базе. Скрипт создаёт корабль `id=1` и таблицу связей с
идентификаторами десантников. Полные данные SpaceMarine остаются в первом сервисе.

Пример выполнения после создания базы:

```bash
psql -h localhost -U starship -d starship -v ON_ERROR_STOP=1 -f db/starship.sql
```

## 3. Переменные окружения

Пример имён находится в `deploy/env.sh`. Задайте их в окружении процесса WildFly
или в локальном файле `deploy/env.local.sh` (он исключён из Git):

```bash
WILDFLY_HOME=/path/to/wildfly
STARSHIP_DB_URL=jdbc:postgresql://localhost:5432/starship
STARSHIP_DB_USER=starship
STARSHIP_DB_PASSWORD=your-database-password
SPACE_MARINE_BASE_URL=https://helios.example.org:8181/api/v1
STARSHIP_TLS_HOST=helios.example.org
STARSHIP_TLS_PASSWORD=your-keystore-password
```

`SPACE_MARINE_BASE_URL` должен указывать на HTTPS-адрес первого сервиса
**с базовым путём `/api/v1`**. `WILDFLY_CONTROLLER` можно задать, если CLI
управления WildFly доступен не по стандартному `localhost:9990`.
`STARSHIP_DB_PASSWORD` можно не задавать, когда на сервере для пользователя
WildFly уже настроен файл `~/.pgpass` с доступом к нужной базе. Драйвер
PostgreSQL прочитает его при подключении.
`STARSHIP_SERVER_BASE_DIR` задаёт отдельный каталог конфигурации, журналов
и развёртываний WildFly. Бинарные файлы остаются в `WILDFLY_HOME`.
Для `STARSHIP_TLS_HOST` подходит DNS-имя или IPv4-адрес сервера. Покупать
домен для самоподписанного сертификата не требуется: имя или IP просто
записывается в SAN сертификата. При переносе на другой адрес создайте новый
`starship.p12` для нового `STARSHIP_TLS_HOST` и настройте доверие клиентов
к его сертификату.

## 4. Доверие сертификату SpaceMarine

Первый сервис использует самоподписанный сертификат. JVM второго сервиса должна
доверять **именно этому** сертификату. Получите публичный сертификат Payara от
администратора первого сервиса и импортируйте его в отдельное хранилище доверия:

```bash
keytool -importcert -noprompt -alias space-marine \
  -file /path/to/space-marine.crt \
  -keystore /path/to/space-marine-truststore.p12 \
  -storetype PKCS12 -storepass 'your-truststore-password'
```

Перед запуском WildFly задайте `JAVA_OPTS` с параметрами
`-Djavax.net.ssl.trustStore=/path/to/space-marine-truststore.p12`,
`-Djavax.net.ssl.trustStoreType=PKCS12` и
`-Djavax.net.ssl.trustStorePassword=your-truststore-password`.
Имя узла в `SPACE_MARINE_BASE_URL` должно присутствовать в SAN сертификата.

## 5. Запуск WildFly и HTTPS

На helios запустите WildFly с переменными из пункта 3:

```bash
bash deploy/starship.sh start
```

После запуска в другом терминале один раз настройте HTTPS:

```bash
bash deploy/starship.sh configure-tls
```

Команда создаёт самоподписанный сертификат Starship в
`$WILDFLY_HOME/standalone/configuration/starship.p12`, регистрирует его в
Elytron, подключает к HTTPS listener Undertow и удаляет стандартный HTTP
listener. Скрипт рассчитан на конфигурацию WildFly с Undertow и Elytron. Если
в WildFly 21 уже есть HTTPS listener со старым `security-realm`, скрипт
заменяет его на listener с `ssl-context=starshipSSC`. Повторный запуск
сохраняет существующий сертификат.

`WEB-INF/web.xml` дополнительно задаёт `CONFIDENTIAL` для всех URL приложения.
Это защита на уровне WAR; фактический запрет HTTP обеспечивает удаление HTTP
listener. Порт HTTPS WildFly по умолчанию — 8443. В предоставленной папке
`wildfly1` заданы другие порты: HTTPS — 21038, HTTP — 21000, управление — 21090.
При работе с этой конфигурацией задайте `WILDFLY_CONTROLLER=127.0.0.1:21090`
и используйте порт 21038 в URL. На удалённом сервере порты зависят от его
конфигурации.
Для доступа извне задайте `STARSHIP_BIND_ADDRESS` равным сетевому адресу
сервера либо `0.0.0.0` и откройте HTTPS-порт в сетевом экране. Локальная
проверка использует `127.0.0.1`, поэтому с других машин к ней подключиться
нельзя.

Пароль хранилища передаётся WildFly CLI и сохраняется в конфигурации Elytron.
Ограничьте доступ к `standalone/configuration/` и `deploy/env.local.sh`.

## 6. Развёртывание и проверка

После настройки HTTPS:

```bash
bash deploy/starship.sh deploy
```

Команда собирает WAR и устанавливает его через `jboss-cli.sh --force`.
`WEB-INF/jboss-web.xml` задаёт корневой context path, поэтому маршрут сервиса:
`https://helios.example.org:8443/api/v1/starship/{starship-id}/...`.

Проверка без изменения данных (некорректный id должен дать HTTP 400):

```bash
curl -k -i -X POST https://helios.example.org:8443/api/v1/starship/0/unload-all
```

HTTP-порт приложения после `configure-tls` не должен принимать соединения.
Флаг `-k` нужен только для ручной проверки самоподписанного сертификата;
межсервисный клиент использует хранилище доверия из пункта 4.

Само развёртывание на helios требует установленного WildFly, доступного CLI,
PostgreSQL и сертификата первого сервиса. Эти ресурсы не поставляются в
репозитории.

## 7. Проверенный запуск на helios

21 октября 2026 года Starship был развёрнут на `helios.cs.ifmo.ru` под
пользователем `s413030`. Перенесённый `~/wildfly1` содержит WAR старого
SpaceMarine в корне `/`. Starship работает в отдельном экземпляре того же
WildFly 21 с каталогом `~/starship-deploy/server`. Старый WAR оставлен на
месте и не отключён.

- HTTPS Starship: `127.0.0.1:21138` на helios; HTTP `21100` отключён.
- Управление WildFly: `127.0.0.1:21190`.
- PostgreSQL: база `studs`, личная схема `s413030`, URL
  `jdbc:postgresql://127.0.0.1:5432/studs?currentSchema=s413030`.
- Пароль БД остаётся в серверном `~/.pgpass`; новый пароль TLS создан на
  сервере в `~/starship-deploy/deploy/env.local.sh` с правами `600`.
- WAR лежит в `~/starship-deploy/server/deployments/starship-service.war`.

Для проверки с локального компьютера откройте туннель:

```bash
ssh -N -p 2222 \
  -L 127.0.0.1:22438:127.0.0.1:21138 \
  -L 127.0.0.1:22490:127.0.0.1:21190 \
  s413030@se.ifmo.ru
```

Пока туннель работает, запрос к Starship:

```bash
curl -k -i -X POST https://localhost:22438/api/v1/starship/1/unload-all
```

Проверены ответы 200, 400 и 404. Погрузка возвращает 503, пока первый
сервис SpaceMarine по настроенному адресу не запущен.
