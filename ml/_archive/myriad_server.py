import io
import base64
from fastapi import FastAPI, HTTPException, File, UploadFile, Form
from pydantic import BaseModel
from typing import List, Optional
from PIL import Image
import torch
import torchvision.transforms.functional as TF

app = FastAPI(title="Myriad Billiard API", description="Trajectories prediction using Flow-Poke-Transformer")

# Global model instance
model = None

# Pydantic schemas for structured requests/responses if needed later
class PokeCoordinate(BaseModel):
    x: float
    y: float
    dx: float
    dy: float

class TrajectoryPoint(BaseModel):
    x: float
    y: float

class TrajectoryResponse(BaseModel):
    points: List[TrajectoryPoint]
    confidence: Optional[float] = 1.0

@app.on_event("startup")
async def load_model():
    global model
    print("Loading MYRIAD Billiards Model via torch.hub...")
    try:
        # Load the model from torch.hub as specified in the README
        model = torch.hub.load("CompVis/myriad", "myriad_billiard")
        model.eval()
        if torch.cuda.is_available():
            model.cuda()
        print("Model loaded successfully.")
    except Exception as e:
        print(f"Failed to load model: {e}")

@app.get("/health")
def health_check():
    return {"status": "ok", "model_loaded": model is not None}

@app.post("/predict", response_model=TrajectoryResponse)
async def predict_trajectory(
    file: UploadFile = File(...),
    poke_x: float = Form(...),
    poke_y: float = Form(...),
    poke_dx: float = Form(...),
    poke_dy: float = Form(...)
):
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded or unavailable")
    
    try:
        # 1. Read and preprocess the image
        contents = await file.read()
        image = Image.open(io.BytesIO(contents)).convert("RGB")
        img_tensor = TF.to_tensor(image).unsqueeze(0) # Shape: (1, 3, H, W)
        
        if torch.cuda.is_available():
            img_tensor = img_tensor.cuda()

        # Normalize coordinates into [-1, 1] range format if required by MYRIAD
        # Often assumed to be normalized flow/coordinates. 
        # This part requires exact API matching with predict_simulate().
        
        # NOTE: MYRIAD's specific parameter format needs verification. 
        # Typical signature expects (images, coordinates, flow_vectors, num_steps)
        # Using a placeholder inference call below.
        
        # Example prediction pseudo-code based on README:
        '''
        with torch.no_grad():
            trajectories = model.predict_simulate(
                img=img_tensor,
                poke_coords=torch.tensor([[[poke_x, poke_y]]]).float().to(img_tensor.device),
                poke_flow=torch.tensor([[[poke_dx, poke_dy]]]).float().to(img_tensor.device),
                steps=30
            )
        '''
        
        # DUMMY implementation for testing integration
        predicted_points = []
        for i in range(10):
            predicted_points.append(TrajectoryPoint(
                x=poke_x + (poke_dx * i * 0.1), 
                y=poke_y + (poke_dy * i * 0.1)
            ))

        return TrajectoryResponse(points=predicted_points, confidence=0.95)

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    # Run with: uvicorn myriad_server:app --reload --host 0.0.0.0 --port 8000
    uvicorn.run("myriad_server:app", host="0.0.0.0", port=8000, reload=True)
