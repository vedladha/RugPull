import { useState, useRef } from 'react';

const ImageUploadBox = ({ onImageUpload }) => {
  const [preview, setPreview] = useState(null);
  const fileInputRef = useRef(null);

  const handleDisplayClick = () => fileInputRef.current.click();

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file && file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreview(reader.result);
        onImageUpload(file); 
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <div 
      onClick={handleDisplayClick}
      className="upload-container"
      style={{
        border: '2px dashed var(--amber)',
        borderRadius: '8px',
        width: '100%',
        height: '200px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: 'pointer',
        overflow: 'hidden',
		backgroundColor: 'var(--black)'
      }}
    >
      <input 
        type="file" 
        ref={fileInputRef} 
        onChange={handleFileChange} 
        accept="image/*" 
        style={{ display: 'none' }} 
      />
      
      {preview ? (
        <img src={preview}
			alt="Preview"
			style={{
				width: '100%',
				height: '100%',
				objectFit: 'contain'
			}} 
		/>
      ) : (
        <p 
			style={{
				fontFamily: 'var(--font-mono)',
				fontSize: '0.85rem',
				color: 'var(--white)'
			}}
		>Click to upload item photo</p>
      )}
    </div>
  );
};

export default ImageUploadBox;