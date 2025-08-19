
import "dotenv/config";
import express from "express";
import cors from "cors";
import morgan from "morgan";
import { S3Client, ListObjectsV2Command, PutObjectCommand, HeadObjectCommand, GetObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import { nanoid } from "nanoid";

const app = express();
app.use(cors());
app.use(morgan("dev"));
app.use(express.json({ limit: "20mb" }));

const PORT = process.env.PORT || 8080;

// Cloudflare R2 config
const R2_ACCOUNT_ID = process.env.R2_ACCOUNT_ID;
const R2_ACCESS_KEY_ID = process.env.R2_ACCESS_KEY_ID;
const R2_SECRET_ACCESS_KEY = process.env.R2_SECRET_ACCESS_KEY;
const R2_BUCKET = process.env.R2_BUCKET;
const R2_PUBLIC_BASEURL = process.env.R2_PUBLIC_BASEURL || "";

if (!R2_ACCOUNT_ID || !R2_ACCESS_KEY_ID || !R2_SECRET_ACCESS_KEY || !R2_BUCKET) {
  console.error("Missing R2 environment variables.");
  process.exit(1);
}

const s3 = new S3Client({
  region: "auto",
  endpoint: `https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com`,
  credentials: {
    accessKeyId: R2_ACCESS_KEY_ID,
    secretAccessKey: R2_SECRET_ACCESS_KEY
  }
});

const pending = new Map();

app.post("/v1/uploads/presign", async (req, res) => {
  try {
    const { name, size, mime } = req.body || {};
    const id = nanoid();
    const key = `uploads/${id}/${encodeURIComponent(name || "file.bin")}`;
    const putCmd = new PutObjectCommand({
      Bucket: R2_BUCKET,
      Key: key,
      ContentType: mime || "application/octet-stream",
      Metadata: { uploadId: id }
    });
    const uploadUrl = await getSignedUrl(s3, putCmd, { expiresIn: 600 });
    pending.set(id, { key, name: name || "file.bin", size, mime, createdAt: Date.now() });
    return res.json({ uploadUrl, uploadId: id, headers: {} });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: "presign_failed" });
  }
});

app.post("/v1/uploads/commit", async (req, res) => {
  try {
    const { uploadId } = req.body || {};
    const meta = pending.get(uploadId);
    if (!meta) return res.status(404).json({ error: "unknown_uploadId" });

    await s3.send(new HeadObjectCommand({ Bucket: R2_BUCKET, Key: meta.key }));

    pending.delete(uploadId);
    const item = {
      id: uploadId,
      name: meta.name,
      size: meta.size,
      mime: meta.mime || "application/octet-stream",
      key: meta.key,
      url: R2_PUBLIC_BASEURL ? `${R2_PUBLIC_BASEURL}/${meta.key}` : "",
      createdAt: Date.now()
    };
    return res.json({ ok: true, item });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: "commit_failed" });
  }
});

app.get("/v1/files", async (_req, res) => {
  try {
    const out = await s3.send(new ListObjectsV2Command({ Bucket: R2_BUCKET, Prefix: "uploads/", MaxKeys: 1000 }));
    const files = (out.Contents || []).map(obj => {
      const key = obj.Key;
      const name = decodeURIComponent(key.split("/").slice(2).join("/"));
      return {
        id: key,
        name,
        size: obj.Size,
        mime: "",
        key,
        url: R2_PUBLIC_BASEURL ? `${R2_PUBLIC_BASEURL}/${key}` : "",
        createdAt: new Date(obj.LastModified || Date.now()).getTime()
      };
    }).sort((a, b) => b.createdAt - a.createdAt);
    return res.json({ files });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: "list_failed" });
  }
});

app.get("/v1/files/presign-get", async (req, res) => {
  try {
    const key = req.query.key;
    if (!key || typeof key !== "string") {
      return res.status(400).json({ error: "missing_key" });
    }
    const getCmd = new GetObjectCommand({ Bucket: R2_BUCKET, Key: key });
    const url = await getSignedUrl(s3, getCmd, { expiresIn: 3600 });
    return res.json({ url, expiresIn: 3600 });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: "presign_get_failed" });
  }
});

app.get("/", (_req, res) => res.send("StreamHive R2 backend is live."));

app.listen(PORT, () => {
  console.log(`StreamHive R2 backend running at http://localhost:${PORT}`);
});
