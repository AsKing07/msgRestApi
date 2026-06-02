# API fichiers de discussion - US8

Ce document décrit les routes Spring Boot pour l'envoi et la gestion des fichiers dans une discussion.

Base URL en local :

```text
http://127.0.0.1:8090
```

Toutes les routes utilisent le header temporaire suivant pour identifier l'utilisateur :

```http
X-User-Id: 1
```

## Donnees de test

Si les autres services `users`, `friends` et `conversations` ne sont pas encore implementes, lance l'API avec le profil `dev-seed` :

```bash
zsh -ic 'sh mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8090 --spring.profiles.active=dev-seed"'
```

Ce profil cree automatiquement :

| Ressource | Valeur |
| --- | --- |
| Alice | `X-User-Id: 1` |
| Bob | `X-User-Id: 2` |
| Discussion Alice/Bob | `conversationId: 1` |

Sans ce profil, il faut deja avoir des utilisateurs et une conversation en base.

## Regles metier

| Action | Autorise |
| --- | --- |
| Envoyer un fichier | Un participant de la discussion |
| Lister les fichiers | Un participant de la discussion |
| Telecharger un fichier | Un participant, uniquement si le fichier est `AVAILABLE` |
| Annuler un fichier | L'expediteur du fichier |
| Supprimer un fichier | L'expediteur du fichier |
| Decliner un fichier | Le destinataire, pas l'expediteur |

Statuts possibles :

```text
AVAILABLE
CANCELLED
DECLINED
DELETED
```

Les fichiers `CANCELLED` et `DECLINED` restent visibles dans la liste avec leur statut. Les fichiers `DELETED` sont masques par la liste standard.

## Routes

| Methode | Route | Description |
| --- | --- | --- |
| `POST` | `/api/conversations/{conversationId}/files` | Envoyer un fichier |
| `GET` | `/api/conversations/{conversationId}/files` | Lister les fichiers de la discussion |
| `GET` | `/api/conversations/{conversationId}/files/{attachmentId}/download` | Telecharger un fichier disponible |
| `POST` | `/api/conversations/{conversationId}/files/{attachmentId}/cancel` | Annuler un fichier envoye |
| `POST` | `/api/conversations/{conversationId}/files/{attachmentId}/decline` | Decliner un fichier recu |
| `DELETE` | `/api/conversations/{conversationId}/files/{attachmentId}` | Supprimer un fichier envoye |

## 1. Envoyer un fichier

Route :

```http
POST /api/conversations/{conversationId}/files
Content-Type: multipart/form-data
X-User-Id: {userId}
```

Dans Postman :

| Champ | Valeur |
| --- | --- |
| Method | `POST` |
| URL | `http://127.0.0.1:8090/api/conversations/1/files` |
| Headers | `X-User-Id: 1` |
| Body | `form-data` |
| Key | `file` |
| Type | `File` |
| Value | choisir un fichier |

Exemple curl :

```bash
curl -F 'file=@pom.xml;type=text/xml' \
  -H 'X-User-Id: 1' \
  http://127.0.0.1:8090/api/conversations/1/files
```

Reponse `201 Created` :

```json
{
  "id": 1,
  "conversationId": 1,
  "uploaderId": 1,
  "originalFileName": "pom.xml",
  "sizeBytes": 6688,
  "contentType": "text/xml",
  "status": "AVAILABLE",
  "uploadedAt": "2026-06-02T12:25:21.922776Z",
  "cancelledAt": null,
  "declinedAt": null,
  "deletedAt": null
}
```

## 2. Lister les fichiers

Route :

```http
GET /api/conversations/{conversationId}/files
X-User-Id: {userId}
```

Exemple curl :

```bash
curl -H 'X-User-Id: 2' \
  http://127.0.0.1:8090/api/conversations/1/files
```

Reponse `200 OK` :

```json
[
  {
    "id": 1,
    "conversationId": 1,
    "uploaderId": 1,
    "originalFileName": "pom.xml",
    "sizeBytes": 6688,
    "contentType": "text/xml",
    "status": "AVAILABLE",
    "uploadedAt": "2026-06-02T12:25:21.922776Z",
    "cancelledAt": null,
    "declinedAt": null,
    "deletedAt": null
  }
]
```

## 3. Telecharger un fichier

Route :

```http
GET /api/conversations/{conversationId}/files/{attachmentId}/download
X-User-Id: {userId}
```

Exemple curl :

```bash
curl -o downloaded.xml \
  -H 'X-User-Id: 2' \
  http://127.0.0.1:8090/api/conversations/1/files/1/download
```

Reponse attendue :

```http
HTTP/1.1 200 OK
Content-Disposition: attachment; filename="pom.xml"
Content-Type: text/xml
Content-Length: 6688
```

Si le fichier est annule, decline ou supprime :

```json
{
  "detail": "Ce fichier n'est plus disponible.",
  "status": 400,
  "title": "Bad Request"
}
```

## 4. Annuler un fichier

Seul l'expediteur peut annuler son fichier.

Route :

```http
POST /api/conversations/{conversationId}/files/{attachmentId}/cancel
X-User-Id: {userId}
```

Exemple curl :

```bash
curl -X POST \
  -H 'X-User-Id: 1' \
  http://127.0.0.1:8090/api/conversations/1/files/2/cancel
```

Reponse `200 OK` :

```json
{
  "id": 2,
  "conversationId": 1,
  "uploaderId": 1,
  "originalFileName": "pom.xml",
  "sizeBytes": 6688,
  "contentType": "text/xml",
  "status": "CANCELLED",
  "uploadedAt": "2026-06-02T12:26:05.695838Z",
  "cancelledAt": "2026-06-02T12:26:14.812603Z",
  "declinedAt": null,
  "deletedAt": null
}
```

## 5. Decliner un fichier

Seul le destinataire peut decliner un fichier recu. L'expediteur ne peut pas decliner son propre fichier.

Route :

```http
POST /api/conversations/{conversationId}/files/{attachmentId}/decline
X-User-Id: {userId}
```

Exemple curl :

```bash
curl -X POST \
  -H 'X-User-Id: 2' \
  http://127.0.0.1:8090/api/conversations/1/files/3/decline
```

Reponse `200 OK` :

```json
{
  "id": 3,
  "conversationId": 1,
  "uploaderId": 1,
  "originalFileName": "pom.xml",
  "sizeBytes": 6688,
  "contentType": "text/xml",
  "status": "DECLINED",
  "uploadedAt": "2026-06-02T12:26:29.116285Z",
  "cancelledAt": null,
  "declinedAt": "2026-06-02T12:26:35.879286Z",
  "deletedAt": null
}
```

## 6. Supprimer un fichier

Seul l'expediteur peut supprimer son fichier. Un fichier supprime n'apparait plus dans la liste standard.

Route :

```http
DELETE /api/conversations/{conversationId}/files/{attachmentId}
X-User-Id: {userId}
```

Exemple curl :

```bash
curl -X DELETE \
  -H 'X-User-Id: 1' \
  http://127.0.0.1:8090/api/conversations/1/files/4
```

Reponse :

```http
HTTP/1.1 204 No Content
```

Si un autre utilisateur essaie de supprimer le fichier :

```json
{
  "detail": "Seul l'expediteur peut supprimer ce fichier.",
  "status": 400,
  "title": "Bad Request"
}
```

## Configuration

Les fichiers physiques sont stockes dans :

```properties
app.file.storage-dir=uploads/files
```

La taille maximale configuree est :

```properties
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=20MB
```

Le dossier `uploads/` est ignore par Git.
