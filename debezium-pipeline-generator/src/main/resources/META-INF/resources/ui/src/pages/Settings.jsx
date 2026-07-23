import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Grid,
  TextField,
  Divider,
  Switch,
  FormControlLabel,
  Tabs,
  Tab,
  Alert,
  AlertTitle,
  IconButton,
  Tooltip,
  Slider,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Chip,
} from '@mui/material';
import {
  Save as SaveIcon,
  Settings as SettingsIcon,
  Psychology as PsychologyIcon,
  Cloud as CloudIcon,
  Security as SecurityIcon,
  Sync as SyncIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import api from '../services/api';

function Settings() {
  const [tab, setTab] = useState(0);
  const [saved, setSaved] = useState(false);

  const [settings, setSettings] = useState({
    general: {
      appName: 'Debezium Pipeline Generator',
      logLevel: 'INFO',
      maxPipelines: 50,
    },
    ai: {
      embeddingProvider: 'minilm',
      llmProvider: 'ollama',
      ollamaUrl: 'http://localhost:11434',
      ollamaModel: 'llama3.1',
      openaiApiKey: '',
      openaiModel: 'gpt-4o-mini',
      confidenceThreshold: 0.3,
      enableMLSuggestions: true,
    },
    kafka: {
      defaultBootstrapServers: 'localhost:9092',
      defaultSchemaRegistry: 'http://localhost:8081',
      defaultTopicPrefix: 'dbserver1',
    },
    deployment: {
      defaultNamespace: 'debezium',
      defaultDeploymentType: 'strimzi',
      strimziVersion: '0.41.0',
      connectImage: 'debezium/connect:3.7',
    },
    security: {
      vaultAddress: '',
      vaultToken: '',
      enableTLS: false,
      certificatePath: '',
    },
  });

  const updateSetting = (section, key, value) => {
    setSettings(prev => ({
      ...prev,
      [section]: { ...prev[section], [key]: value },
    }));
  };

  const saveSettings = async () => {
    try {
      // await api.post('/settings', settings);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (err) {
      console.error('Failed to save settings', err);
    }
  };

  const SettingSection = ({ icon, title, children }) => (
    <Card sx={{ mb: 3, borderRadius: 2 }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
          {icon}
          <Typography variant="h6">{title}</Typography>
        </Box>
        <Divider sx={{ mb: 3 }} />
        {children}
      </CardContent>
    </Card>
  );

  return (
    <Box sx={{ maxWidth: '800px', mx: 'auto' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>Settings</Typography>
          <Typography variant="body2" color="text.secondary">Configure the pipeline generator and AI services</Typography>
        </Box>
        <Button variant="contained" startIcon={<SaveIcon />} onClick={saveSettings}>
          {saved ? 'Saved!' : 'Save Settings'}
        </Button>
      </Box>

      {saved && (
        <Alert severity="success" sx={{ mb: 3 }}>
          <AlertTitle>Settings Saved</AlertTitle>
          Settings have been updated successfully.
        </Alert>
      )}

      <Tabs value={tab} onChange={(e, v) => setTab(v)} sx={{ mb: 3 }} variant="fullWidth">
        <Tab label="General" />
        <Tab label="AI & Embeddings" />
        <Tab label="Kafka & Schema" />
        <Tab label="Deployment" />
        <Tab label="Security" />
      </Tabs>

      {tab === 0 && (
        <SettingSection icon={<SettingsIcon color="primary" />} title="General Settings">
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Application Name" value={settings.general.appName} onChange={(e) => updateSetting('general', 'appName', e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Log Level</InputLabel>
                <Select value={settings.general.logLevel} onChange={(e) => updateSetting('general', 'logLevel', e.target.value)} label="Log Level">
                  <MenuItem value="DEBUG">DEBUG</MenuItem>
                  <MenuItem value="INFO">INFO</MenuItem>
                  <MenuItem value="WARN">WARN</MenuItem>
                  <MenuItem value="ERROR">ERROR</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Max Pipelines" type="number" value={settings.general.maxPipelines} onChange={(e) => updateSetting('general', 'maxPipelines', parseInt(e.target.value))} />
            </Grid>
          </Grid>
        </SettingSection>
      )}

      {tab === 1 && (
        <SettingSection icon={<PsychologyIcon color="primary" />} title="AI & Embeddings Configuration">
          <FormControlLabel control={<Switch checked={settings.ai.enableMLSuggestions} onChange={(e) => updateSetting('ai', 'enableMLSuggestions', e.target.checked)} />} label="Enable ML-based mapping suggestions" sx={{ mb: 2 }} />
          <Typography variant="subtitle2" gutterBottom>Confidence Threshold: {Math.round(settings.ai.confidenceThreshold * 100)}%</Typography>
          <Slider value={settings.ai.confidenceThreshold} onChange={(e, v) => updateSetting('ai', 'confidenceThreshold', v)} min={0} max={1} step={0.05} sx={{ mb: 3 }} />
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Embedding Provider</InputLabel>
                <Select value={settings.ai.embeddingProvider} onChange={(e) => updateSetting('ai', 'embeddingProvider', e.target.value)} label="Embedding Provider">
                  <MenuItem value="minilm">MiniLM-L6-v2 (Local)</MenuItem>
                  <MenuItem value="huggingface">Hugging Face</MenuItem>
                  <MenuItem value="ollama">Ollama (Local)</MenuItem>
                  <MenuItem value="voyageai">Voyage AI</MenuItem>
                  <MenuItem value="openai">OpenAI</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>LLM Provider</InputLabel>
                <Select value={settings.ai.llmProvider} onChange={(e) => updateSetting('ai', 'llmProvider', e.target.value)} label="LLM Provider">
                  <MenuItem value="ollama">Ollama (Local)</MenuItem>
                  <MenuItem value="openai">OpenAI</MenuItem>
                  <MenuItem value="anthropic">Anthropic Claude</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Ollama URL" value={settings.ai.ollamaUrl} onChange={(e) => updateSetting('ai', 'ollamaUrl', e.target.value)} placeholder="http://localhost:11434" />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Ollama Model" value={settings.ai.ollamaModel} onChange={(e) => updateSetting('ai', 'ollamaModel', e.target.value)} placeholder="llama3.1" />
            </Grid>
          </Grid>
        </SettingSection>
      )}

      {tab === 2 && (
        <SettingSection icon={<SyncIcon color="primary" />} title="Kafka & Schema Registry">
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Default Bootstrap Servers" value={settings.kafka.defaultBootstrapServers} onChange={(e) => updateSetting('kafka', 'defaultBootstrapServers', e.target.value)} placeholder="localhost:9092" />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Default Schema Registry URL" value={settings.kafka.defaultSchemaRegistry} onChange={(e) => updateSetting('kafka', 'defaultSchemaRegistry', e.target.value)} placeholder="http://localhost:8081" />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Default Topic Prefix" value={settings.kafka.defaultTopicPrefix} onChange={(e) => updateSetting('kafka', 'defaultTopicPrefix', e.target.value)} placeholder="dbserver1" />
            </Grid>
          </Grid>
        </SettingSection>
      )}

      {tab === 3 && (
        <SettingSection icon={<CloudIcon color="primary" />} title="Deployment Configuration">
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Default Namespace" value={settings.deployment.defaultNamespace} onChange={(e) => updateSetting('deployment', 'defaultNamespace', e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Default Deployment Type</InputLabel>
                <Select value={settings.deployment.defaultDeploymentType} onChange={(e) => updateSetting('deployment', 'defaultDeploymentType', e.target.value)} label="Default Deployment Type">
                  <MenuItem value="strimzi">Strimzi Operator</MenuItem>
                  <MenuItem value="docker-compose">Docker Compose</MenuItem>
                  <MenuItem value="helm">Helm Charts</MenuItem>
                  <MenuItem value="terraform">Terraform</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Strimzi Version" value={settings.deployment.strimziVersion} onChange={(e) => updateSetting('deployment', 'strimziVersion', e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Connect Image" value={settings.deployment.connectImage} onChange={(e) => updateSetting('deployment', 'connectImage', e.target.value)} />
            </Grid>
          </Grid>
        </SettingSection>
      )}

      {tab === 4 && (
        <SettingSection icon={<SecurityIcon color="primary" />} title="Security Configuration">
          <FormControlLabel control={<Switch checked={settings.security.enableTLS} onChange={(e) => updateSetting('security', 'enableTLS', e.target.checked)} />} label="Enable TLS" sx={{ mb: 2 }} />
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Vault Address" value={settings.security.vaultAddress} onChange={(e) => updateSetting('security', 'vaultAddress', e.target.value)} placeholder="http://vault:8200" />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Vault Token" type="password" value={settings.security.vaultToken} onChange={(e) => updateSetting('security', 'vaultToken', e.target.value)} placeholder="********" />
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Certificate Path" value={settings.security.certificatePath} onChange={(e) => updateSetting('security', 'certificatePath', e.target.value)} placeholder="/etc/certs/debezium.pem" />
            </Grid>
          </Grid>
        </SettingSection>
      )}
    </Box>
  );
}

export default Settings;