from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .routers import lab_manager, librarian

app = FastAPI(
    openapi_url="/api/openapi.json", docs_url="/api/docs", redoc_url="/api/redoc"
)

# TODO: use .env to set origins and CORS
origins = [
    "http://localhost",
    "http://localhost:3000",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(librarian.router)
app.include_router(lab_manager.router)
