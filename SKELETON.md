The objective is to set up a minimal version of the app that proves the core components can communicating. The stack consists of:

- **Frontend:** React with Vite  
- **Backend:** SpringBoot  
- **Database:** MySQL  
- **Crypto:** Hedera Hashgraph (for future functionality)

### Steps:

1. **Frontend:** A simple form will collect data and send it to the backend.
2. **Backend:** A SpringBoot API will process the data, store it in MySQL, and retrieve it when requested.
3. **Database:** MySQL will hold the data (e.g., names or items).
4. **Hedera Hashgraph:** Initially set up in the backend for future blockchain features.

The flow will be:  
- **Frontend → Backend → Database → Backend → Frontend.**

The primary goal is ensuring these components interact. The backend will receive data from the frontend, store it in MySQL, retrieve it, and send it back to the frontend. The Hedera setup will be ready for future use.

This setup lays the foundation for expanding features and functionality once the basic integration works.