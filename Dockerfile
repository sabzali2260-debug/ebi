FROM node:22-alpine AS build
WORKDIR /work
RUN apk add --no-cache unzip patch
COPY source-parts/ ./source-parts/
COPY patch-parts/ ./patch-parts/
RUN cat source-parts/01 source-parts/02 source-parts/03 source-parts/04 source-parts/05 source-parts/06 source-parts/07 source-parts/08a source-parts/08b source-parts/08c source-parts/08d source-parts/09 | base64 -d > /tmp/source.zip && unzip -q /tmp/source.zip -d /work/project
RUN cat patch-parts/01 patch-parts/02 | base64 -d | gzip -d | patch -p1 -d /work/project
WORKDIR /work/project/do-nafar-cinema
RUN npm install && npm --prefix client install && npm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /work/project/do-nafar-cinema/package*.json ./
RUN npm install --omit=dev
COPY --from=build /work/project/do-nafar-cinema/server ./server
COPY --from=build /work/project/do-nafar-cinema/client/dist ./client/dist
RUN mkdir -p /app/server/uploads
EXPOSE 10000
CMD ["npm", "start"]
