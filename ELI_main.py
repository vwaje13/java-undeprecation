import httpx
import os
from dotenv import load_dotenv

# Load .env file
load_dotenv()

# Get the token
# jwt_token = os.getenv("ELI_API_KEY")
jwt_token = "eli-58e256f2-2d43-488f-9626-ffa03ed96d84"

ELI_API_URL = "https://portal.eli.gaia.gic.ericsson.se"
headers = {"accept": "application/json",
           "Authorization": f"Bearer {jwt_token}"
           }
with httpx.Client(verify=False) as client:
    eli_resp = client.get(
        f"https://portal.eli.gaia.gic.ericsson.se/api/v1/llm/list", headers=headers
        )
    print(ELI_API_URL)
    print(eli_resp.json())