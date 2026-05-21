"""
agent.py — This is for the agent's behaviour - will use Claude as a base before establishing
what we want to proceed with changes
Privacy is key.

What is needed:
- No data leaves machine (remote server) except to the Anthropic API and dedicated APIs
- Credentials are never stored in plain text - worry about later
- No third party extensions* will need to confirm safety of OpenClaw
- Minimal permissions
- Conversation history is held in memory - don't write to disk except for important info
"""

# add logging for error checks https://docs.python.org/3/library/logging.html
import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s", #AI recommended this format
    handlers=[
        logging.FileHandler("agent_activity.log"),
        logging.StreamHandler(),
    ],
)
log = logging.getLogger(__name__)

# Constants
MODEL = "claude-sonnet-4-6" # model type
MAX_TOKENS = 1024 # limit cost of messages
MAX_HISTORY = 20

class Agent:
    # initialize the config file
    # initialize the api key for the agent
    # initialize the local memory and temp memory
    # log it
    def __init__(self):
        pass

    